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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.dto.UserDTO;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.CourseRepository;
import com.example.demo.repository.EnrollmentRepository;
import com.example.demo.repository.QuizRepository;
import com.example.demo.repository.QuizResultRepository;
import com.example.demo.service.UserService;

import jakarta.validation.Valid;

/**
 * Controller for Super Administrator (platform owner) functionality.
 * Super Admin can:
 * - View platform-wide statistics and activity
 * - Manage teachers (CRUD)
 * - Monitor platform health
 * 
 * Super Admin CANNOT modify teacher's courses - supervision only.
 * All endpoints require ADMINISTRATOR role.
 */
@Controller
@RequestMapping("/superadmin")
@PreAuthorize("hasRole('ADMINISTRATOR')")
public class SuperAdminController {

    private final UserService userService;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final QuizRepository quizRepository;
    private final QuizResultRepository quizResultRepository;

    public SuperAdminController(UserService userService,
                                CourseRepository courseRepository,
                                EnrollmentRepository enrollmentRepository,
                                QuizRepository quizRepository,
                                QuizResultRepository quizResultRepository) {
        this.userService = userService;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.quizRepository = quizRepository;
        this.quizResultRepository = quizResultRepository;
    }

    // ========== Dashboard ==========

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // Platform-wide statistics
        long totalTeachers = userService.countTeachers();
        long totalStudents = userService.countStudents();
        long totalCourses = courseRepository.count();
        long totalEnrollments = enrollmentRepository.count();
        long totalQuizzes = quizRepository.count();
        long totalQuizResults = quizResultRepository.count();
        
        model.addAttribute("totalTeachers", totalTeachers);
        model.addAttribute("totalStudents", totalStudents);
        model.addAttribute("totalCourses", totalCourses);
        model.addAttribute("totalEnrollments", totalEnrollments);
        model.addAttribute("totalQuizzes", totalQuizzes);
        model.addAttribute("totalQuizResults", totalQuizResults);
        
        // Recent activity
        model.addAttribute("recentTeachers", userService.findAllTeachers().stream().limit(5).toList());
        model.addAttribute("recentStudents", userService.findAllStudents().stream().limit(5).toList());
        
        return "superadmin/dashboard";
    }

    // ========== Teacher Management ==========

    @GetMapping("/teachers")
    public String listTeachers(Model model) {
        List<User> teachers = userService.findAllTeachers();
        model.addAttribute("teachers", teachers);
        return "superadmin/teachers/list";
    }

    @GetMapping("/teachers/new")
    public String newTeacherForm(Model model) {
        UserDTO userDTO = new UserDTO();
        userDTO.setRole(Role.TEACHER);
        userDTO.setEnabled(true);
        model.addAttribute("teacher", userDTO);
        return "superadmin/teachers/form";
    }

    @PostMapping("/teachers/new")
    public String createTeacher(@Valid @ModelAttribute("teacher") UserDTO userDTO,
                                BindingResult result,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "superadmin/teachers/form";
        }

        try {
            userService.createTeacher(userDTO);
            redirectAttributes.addFlashAttribute("success", "Teacher created successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/superadmin/teachers/new";
        }

        return "redirect:/superadmin/teachers";
    }

    @GetMapping("/teachers/{id}/edit")
    public String editTeacherForm(@PathVariable Long id, Model model) {
        User teacher = userService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found"));
        
        if (teacher.getRole() != Role.TEACHER) {
            throw new IllegalArgumentException("User is not a teacher");
        }
        
        model.addAttribute("teacher", userService.toDTO(teacher));
        return "superadmin/teachers/form";
    }

    @PostMapping("/teachers/{id}/edit")
    public String updateTeacher(@PathVariable Long id,
                                @Valid @ModelAttribute("teacher") UserDTO userDTO,
                                BindingResult result,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "superadmin/teachers/form";
        }

        try {
            // Ensure the role stays as TEACHER
            userDTO.setRole(Role.TEACHER);
            userService.updateUser(id, userDTO);
            redirectAttributes.addFlashAttribute("success", "Teacher updated successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/superadmin/teachers/" + id + "/edit";
        }

        return "redirect:/superadmin/teachers";
    }

    @PostMapping("/teachers/{id}/delete")
    public String deleteTeacher(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            User teacher = userService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Teacher not found"));
            
            if (teacher.getRole() != Role.TEACHER) {
                throw new IllegalArgumentException("User is not a teacher");
            }
            
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("success", "Teacher deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete teacher: " + e.getMessage());
        }
        return "redirect:/superadmin/teachers";
    }

    @PostMapping("/teachers/{id}/toggle-status")
    public String toggleTeacherStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            User teacher = userService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Teacher not found"));
            
            if (teacher.getRole() != Role.TEACHER) {
                throw new IllegalArgumentException("User is not a teacher");
            }
            
            UserDTO dto = userService.toDTO(teacher);
            dto.setEnabled(!teacher.isEnabled());
            userService.updateUser(id, dto);
            
            String status = dto.isEnabled() ? "enabled" : "disabled";
            redirectAttributes.addFlashAttribute("success", "Teacher " + status + " successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/superadmin/teachers";
    }

    // ========== Activity Monitoring ==========

    @GetMapping("/activity")
    public String viewActivity(Model model) {
        // Summary of platform activity
        model.addAttribute("totalQuizzesTaken", quizResultRepository.count());
        model.addAttribute("recentQuizResults", quizResultRepository.findAll().stream().limit(10).toList());
        
        return "superadmin/activity";
    }
}
