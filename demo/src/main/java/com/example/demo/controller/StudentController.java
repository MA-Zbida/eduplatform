package com.example.demo.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.dto.DashboardStatsDTO;
import com.example.demo.dto.QuizRequestDTO;
import com.example.demo.dto.QuizSubmissionDTO;
import com.example.demo.entity.Course;
import com.example.demo.entity.DifficultyLevel;
import com.example.demo.entity.Enrollment;
import com.example.demo.entity.Question;
import com.example.demo.entity.Quiz;
import com.example.demo.entity.QuizResult;
import com.example.demo.security.SecurityUtils;
import com.example.demo.service.AgentService;
import com.example.demo.service.CourseService;
import com.example.demo.service.DashboardService;
import com.example.demo.service.EnrollmentService;
import com.example.demo.service.QuizService;

/**
 * Controller for student functionality.
 * All endpoints require STUDENT role.
 */
@Controller
@RequestMapping("/student")
@PreAuthorize("hasRole('STUDENT')")
public class StudentController {

    private final SecurityUtils securityUtils;
    private final CourseService courseService;
    private final EnrollmentService enrollmentService;
    private final QuizService quizService;
    private final DashboardService dashboardService;
    private final AgentService agentService;

    public StudentController(SecurityUtils securityUtils,
                             CourseService courseService,
                             EnrollmentService enrollmentService,
                             QuizService quizService,
                             DashboardService dashboardService,
                             AgentService agentService) {
        this.securityUtils = securityUtils;
        this.courseService = courseService;
        this.enrollmentService = enrollmentService;
        this.quizService = quizService;
        this.dashboardService = dashboardService;
        this.agentService = agentService;
    }

    // ========== Dashboard ==========

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Long studentId = securityUtils.getCurrentUserId();
        DashboardStatsDTO stats = dashboardService.getStudentDashboardStats(studentId);
        List<Enrollment> enrollments = enrollmentService.findByStudent(studentId);
        List<QuizResult> recentResults = quizService.findResultsByStudent(studentId).stream().limit(5).toList();

        model.addAttribute("stats", stats);
        model.addAttribute("enrollments", enrollments);
        model.addAttribute("recentResults", recentResults);
        model.addAttribute("user", securityUtils.getCurrentUser());
        return "student/dashboard";
    }

    // ========== Courses ==========

    @GetMapping("/courses")
    public String listCourses(Model model) {
        Long studentId = securityUtils.getCurrentUserId();
        List<Enrollment> enrollments = enrollmentService.findByStudent(studentId);
        model.addAttribute("enrollments", enrollments);
        return "student/courses/list";
    }

    @GetMapping("/courses/{id}")
    public String viewCourse(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Long studentId = securityUtils.getCurrentUserId();

        // Verify enrollment
        if (!enrollmentService.isEnrolled(studentId, id)) {
            redirectAttributes.addFlashAttribute("error", "You are not enrolled in this course.");
            return "redirect:/student/courses";
        }

        Course course = courseService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));
        Enrollment enrollment = enrollmentService.findByStudentAndCourse(studentId, id)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found"));
        List<Quiz> quizzes = quizService.findByStudentAndCourse(studentId, id);
        List<String> recommendations = agentService.getStudentRecommendations(studentId, id);

        model.addAttribute("course", course);
        model.addAttribute("enrollment", enrollment);
        model.addAttribute("quizzes", quizzes);
        model.addAttribute("recommendations", recommendations);
        model.addAttribute("difficulties", DifficultyLevel.values());
        return "student/courses/view";
    }

    // ========== Quizzes ==========

    @GetMapping("/quizzes")
    public String listQuizzes(Model model) {
        Long studentId = securityUtils.getCurrentUserId();
        List<Quiz> quizzes = quizService.findByStudent(studentId);
        model.addAttribute("quizzes", quizzes);
        return "student/quizzes/list";
    }

    @PostMapping("/courses/{courseId}/quiz/generate")
    public String generateQuiz(@PathVariable Long courseId,
                               @RequestParam(defaultValue = "5") int numberOfQuestions,
                               @RequestParam(required = false) DifficultyLevel difficulty,
                               RedirectAttributes redirectAttributes) {
        Long studentId = securityUtils.getCurrentUserId();

        // Verify enrollment
        if (!enrollmentService.isEnrolled(studentId, courseId)) {
            redirectAttributes.addFlashAttribute("error", "You are not enrolled in this course.");
            return "redirect:/student/courses";
        }

        try {
            QuizRequestDTO request = new QuizRequestDTO(courseId, numberOfQuestions, difficulty);
            Quiz quiz = quizService.generateQuiz(studentId, request);
            return "redirect:/student/quizzes/" + quiz.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to generate quiz: " + e.getMessage());
            return "redirect:/student/courses/" + courseId;
        }
    }

    @GetMapping("/quizzes/{id}")
    public String viewQuiz(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Long studentId = securityUtils.getCurrentUserId();

        Quiz quiz = quizService.findByIdWithQuestions(id);
        if (quiz == null) {
            redirectAttributes.addFlashAttribute("error", "Quiz not found.");
            return "redirect:/student/quizzes";
        }

        // Verify ownership
        if (!quiz.getStudent().getId().equals(studentId)) {
            redirectAttributes.addFlashAttribute("error", "This quiz does not belong to you.");
            return "redirect:/student/quizzes";
        }

        // If quiz is already completed, show results
        if (quiz.getResult() != null) {
            return "redirect:/student/quizzes/" + id + "/result";
        }

        List<Question> questions = quizService.findQuestionsByQuizId(id);
        model.addAttribute("quiz", quiz);
        model.addAttribute("questions", questions);
        return "student/quizzes/take";
    }

    @PostMapping("/quizzes/{id}/submit")
    public String submitQuiz(@PathVariable Long id,
                             @RequestParam Map<String, String> formParams,
                             @RequestParam int timeTaken,
                             RedirectAttributes redirectAttributes) {
        Long studentId = securityUtils.getCurrentUserId();

        try {
            // Parse answers from form
            Map<Long, Integer> answers = new HashMap<>();
            for (Map.Entry<String, String> entry : formParams.entrySet()) {
                if (entry.getKey().startsWith("answer_")) {
                    Long questionId = Long.parseLong(entry.getKey().replace("answer_", ""));
                    Integer selectedOption = Integer.parseInt(entry.getValue());
                    answers.put(questionId, selectedOption);
                }
            }

            QuizSubmissionDTO submission = new QuizSubmissionDTO(id, answers, timeTaken);
            quizService.submitQuiz(studentId, submission);

            return "redirect:/student/quizzes/" + id + "/result";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to submit quiz: " + e.getMessage());
            return "redirect:/student/quizzes/" + id;
        }
    }

    @GetMapping("/quizzes/{id}/result")
    public String viewQuizResult(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Long studentId = securityUtils.getCurrentUserId();

        Quiz quiz = quizService.findByIdWithQuestions(id);
        if (quiz == null) {
            redirectAttributes.addFlashAttribute("error", "Quiz not found.");
            return "redirect:/student/quizzes";
        }

        // Verify ownership
        if (!quiz.getStudent().getId().equals(studentId)) {
            redirectAttributes.addFlashAttribute("error", "This quiz does not belong to you.");
            return "redirect:/student/quizzes";
        }

        QuizResult result = quizService.findResultByQuizId(id)
                .orElse(null);

        if (result == null) {
            redirectAttributes.addFlashAttribute("error", "Quiz has not been submitted yet.");
            return "redirect:/student/quizzes/" + id;
        }

        List<Question> questions = quizService.findQuestionsByQuizId(id);

        model.addAttribute("quiz", quiz);
        model.addAttribute("result", result);
        model.addAttribute("questions", questions);
        return "student/quizzes/result";
    }

    // ========== Quiz History ==========

    @GetMapping("/history")
    public String quizHistory(Model model) {
        Long studentId = securityUtils.getCurrentUserId();
        List<QuizResult> results = quizService.findResultsByStudent(studentId);
        Double averageScore = quizService.getAverageScoreByStudent(studentId);
        long passedCount = quizService.countPassedByStudent(studentId);

        model.addAttribute("results", results);
        model.addAttribute("averageScore", averageScore != null ? averageScore : 0.0);
        model.addAttribute("passedCount", passedCount);
        model.addAttribute("totalCount", results.size());
        return "student/history";
    }
}
