package com.example.demo.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.dto.CourseDTO;
import com.example.demo.dto.DashboardStatsDTO;
import com.example.demo.dto.UserDTO;
import com.example.demo.entity.Course;
import com.example.demo.entity.Enrollment;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.service.CourseService;
import com.example.demo.service.DashboardService;
import com.example.demo.service.EnrollmentService;
import com.example.demo.service.UserService;

import jakarta.validation.Valid;

/**
 * Controller for administrator functionality.
 * All endpoints require ADMINISTRATOR role.
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMINISTRATOR')")
public class AdminController {

    private final UserService userService;
    private final CourseService courseService;
    private final EnrollmentService enrollmentService;
    private final DashboardService dashboardService;

    public AdminController(UserService userService,
                           CourseService courseService,
                           EnrollmentService enrollmentService,
                           DashboardService dashboardService) {
        this.userService = userService;
        this.courseService = courseService;
        this.enrollmentService = enrollmentService;
        this.dashboardService = dashboardService;
    }

    // ========== Dashboard ==========

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        DashboardStatsDTO stats = dashboardService.getAdminDashboardStats();
        model.addAttribute("stats", stats);
        model.addAttribute("recentCourses", courseService.findAllCourses().stream().limit(5).toList());
        model.addAttribute("recentStudents", userService.findAllStudents().stream().limit(5).toList());
        return "admin/dashboard";
    }

    // ========== Student Management ==========

    @GetMapping("/students")
    public String listStudents(Model model) {
        List<User> students = userService.findAllStudents();
        model.addAttribute("students", students);
        return "admin/students/list";
    }

    @GetMapping("/students/new")
    public String newStudentForm(Model model) {
        UserDTO userDTO = new UserDTO();
        userDTO.setRole(Role.STUDENT);
        userDTO.setEnabled(true);
        model.addAttribute("student", userDTO);
        return "admin/students/form";
    }

    @PostMapping("/students/new")
    public String createStudent(@Valid @ModelAttribute("student") UserDTO userDTO,
                                BindingResult result,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/students/form";
        }

        try {
            userService.createStudent(userDTO);
            redirectAttributes.addFlashAttribute("success", "Student created successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/students/new";
        }

        return "redirect:/admin/students";
    }

    @GetMapping("/students/{id}/edit")
    public String editStudentForm(@PathVariable Long id, Model model) {
        User student = userService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        model.addAttribute("student", userService.toDTO(student));
        return "admin/students/form";
    }

    @PostMapping("/students/{id}/edit")
    public String updateStudent(@PathVariable Long id,
                                @Valid @ModelAttribute("student") UserDTO userDTO,
                                BindingResult result,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/students/form";
        }

        try {
            userService.updateUser(id, userDTO);
            redirectAttributes.addFlashAttribute("success", "Student updated successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/students/" + id + "/edit";
        }

        return "redirect:/admin/students";
    }

    @PostMapping("/students/{id}/delete")
    public String deleteStudent(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("success", "Student deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete student: " + e.getMessage());
        }
        return "redirect:/admin/students";
    }

    // ========== Course Management ==========

    @GetMapping("/courses")
    public String listCourses(Model model) {
        List<Course> courses = courseService.findAllCourses();
        model.addAttribute("courses", courses);
        return "admin/courses/list";
    }

    @GetMapping("/courses/new")
    public String newCourseForm(Model model) {
        model.addAttribute("course", new CourseDTO());
        return "admin/courses/form";
    }

    @PostMapping("/courses/new")
    public String createCourse(@Valid @ModelAttribute("course") CourseDTO courseDTO,
                               BindingResult result,
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/courses/form";
        }

        try {
            courseService.createCourse(courseDTO);
            redirectAttributes.addFlashAttribute("success", "Course created successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/courses/new";
        }

        return "redirect:/admin/courses";
    }

    @GetMapping("/courses/{id}")
    public String viewCourse(@PathVariable Long id, Model model) {
        Course course = courseService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));
        List<Enrollment> enrollments = enrollmentService.findByCourse(id);
        List<User> availableStudents = userService.findStudentsNotEnrolledInCourse(id);

        model.addAttribute("course", course);
        model.addAttribute("enrollments", enrollments);
        model.addAttribute("availableStudents", availableStudents);
        return "admin/courses/view";
    }

    @GetMapping("/courses/{id}/edit")
    public String editCourseForm(@PathVariable Long id, Model model) {
        Course course = courseService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));
        model.addAttribute("course", courseService.toDTO(course));
        return "admin/courses/form";
    }

    @PostMapping("/courses/{id}/edit")
    public String updateCourse(@PathVariable Long id,
                               @Valid @ModelAttribute("course") CourseDTO courseDTO,
                               BindingResult result,
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/courses/form";
        }

        try {
            courseService.updateCourse(id, courseDTO);
            redirectAttributes.addFlashAttribute("success", "Course updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/courses/" + id + "/edit";
        }

        return "redirect:/admin/courses";
    }

    @PostMapping("/courses/{id}/delete")
    public String deleteCourse(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            courseService.deleteCourse(id);
            redirectAttributes.addFlashAttribute("success", "Course deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete course: " + e.getMessage());
        }
        return "redirect:/admin/courses";
    }

    @PostMapping("/courses/{id}/publish")
    public String publishCourse(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            courseService.publishCourse(id);
            redirectAttributes.addFlashAttribute("success", "Course published successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/courses/" + id;
    }

    @PostMapping("/courses/{id}/index")
    public String indexCourse(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            courseService.indexCourseForRAG(id);
            redirectAttributes.addFlashAttribute("success", "Course indexed for AI quiz generation!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/courses/" + id;
    }

    // ========== Enrollment Management ==========

    @PostMapping("/courses/{courseId}/enroll")
    public String enrollStudent(@PathVariable Long courseId,
                                @RequestParam Long studentId,
                                RedirectAttributes redirectAttributes) {
        try {
            enrollmentService.enrollStudent(studentId, courseId);
            redirectAttributes.addFlashAttribute("success", "Student enrolled successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/courses/" + courseId;
    }

    @PostMapping("/courses/{courseId}/unenroll/{studentId}")
    public String unenrollStudent(@PathVariable Long courseId,
                                  @PathVariable Long studentId,
                                  RedirectAttributes redirectAttributes) {
        try {
            enrollmentService.unenrollStudent(studentId, courseId);
            redirectAttributes.addFlashAttribute("success", "Student unenrolled successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/courses/" + courseId;
    }
}
