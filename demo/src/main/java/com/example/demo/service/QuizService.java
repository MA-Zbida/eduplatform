package com.example.demo.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.QuizRequestDTO;
import com.example.demo.dto.QuizSubmissionDTO;
import com.example.demo.entity.Course;
import com.example.demo.entity.Question;
import com.example.demo.entity.Quiz;
import com.example.demo.entity.QuizResult;
import com.example.demo.entity.User;
import com.example.demo.repository.CourseRepository;
import com.example.demo.repository.EnrollmentRepository;
import com.example.demo.repository.QuestionRepository;
import com.example.demo.repository.QuizRepository;
import com.example.demo.repository.QuizResultRepository;
import com.example.demo.repository.UserRepository;

/**
 * Service class for Quiz entity operations.
 * Handles quiz generation, retrieval, and result submission.
 */
@Service
@Transactional
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuizResultRepository quizResultRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AgentService agentService;

    public QuizService(QuizRepository quizRepository,
                       QuestionRepository questionRepository,
                       QuizResultRepository quizResultRepository,
                       CourseRepository courseRepository,
                       UserRepository userRepository,
                       EnrollmentRepository enrollmentRepository,
                       AgentService agentService) {
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.quizResultRepository = quizResultRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.agentService = agentService;
    }

    /**
     * Generate a new quiz for a student using the Agentic AI.
     */
    public Quiz generateQuiz(Long studentId, QuizRequestDTO request) {
        // Validate student
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        if (!student.isStudent()) {
            throw new SecurityException("Only students can request quizzes");
        }

        // Validate course
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + request.getCourseId()));

        // Verify enrollment
        if (!enrollmentRepository.existsByStudentIdAndCourseId(studentId, request.getCourseId())) {
            throw new SecurityException("Student is not enrolled in this course");
        }

        // Verify course is indexed for RAG
        if (!course.isIndexed()) {
            throw new IllegalStateException("Course is not indexed for quiz generation. Please contact administrator.");
        }

        // Use Agentic AI to generate the quiz
        return agentService.generateQuiz(student, course, request);
    }

    /**
     * Submit quiz answers and get results evaluated by the Agentic AI.
     */
    public QuizResult submitQuiz(Long studentId, QuizSubmissionDTO submission) {
        Quiz quiz = quizRepository.findById(submission.getQuizId())
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found: " + submission.getQuizId()));

        // Verify ownership
        if (!quiz.getStudent().getId().equals(studentId)) {
            throw new SecurityException("This quiz does not belong to the student");
        }

        // Verify not already submitted
        if (quiz.getResult() != null) {
            throw new IllegalStateException("Quiz has already been submitted");
        }

        // Use Agentic AI to evaluate the quiz
        return agentService.evaluateQuiz(quiz, submission);
    }

    @Transactional(readOnly = true)
    public Optional<Quiz> findById(Long id) {
        return quizRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Quiz findByIdWithQuestions(Long id) {
        return quizRepository.findByIdWithQuestions(id);
    }

    @Transactional(readOnly = true)
    public List<Quiz> findByStudent(Long studentId) {
        return quizRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
    }

    @Transactional(readOnly = true)
    public List<Quiz> findByStudentAndCourse(Long studentId, Long courseId) {
        return quizRepository.findByStudentIdAndCourseId(studentId, courseId);
    }

    @Transactional(readOnly = true)
    public List<Question> findQuestionsByQuizId(Long quizId) {
        return questionRepository.findByQuizIdWithOptions(quizId);
    }

    @Transactional(readOnly = true)
    public Optional<QuizResult> findResultByQuizId(Long quizId) {
        return quizResultRepository.findByQuizId(quizId);
    }

    @Transactional(readOnly = true)
    public List<QuizResult> findResultsByStudent(Long studentId) {
        return quizResultRepository.findByStudentIdOrderByCompletedAtDesc(studentId);
    }

    @Transactional(readOnly = true)
    public List<QuizResult> findResultsByCourseAndStudent(Long courseId, Long studentId) {
        return quizResultRepository.findByCourseIdAndStudentId(courseId, studentId);
    }

    @Transactional(readOnly = true)
    public long countQuizzes() {
        return quizRepository.count();
    }

    @Transactional(readOnly = true)
    public Double getAverageScoreByStudent(Long studentId) {
        return quizResultRepository.getAverageScoreByStudentId(studentId);
    }

    @Transactional(readOnly = true)
    public long countPassedByStudent(Long studentId) {
        return quizResultRepository.countPassedByStudentId(studentId);
    }
}
