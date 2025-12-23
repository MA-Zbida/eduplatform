package com.example.demo.controller;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.dto.CourseDTO;
import com.example.demo.dto.DashboardStatsDTO;
import com.example.demo.dto.ModuleDTO;
import com.example.demo.dto.UserDTO;
import com.example.demo.entity.Course;
import com.example.demo.entity.Enrollment;
import com.example.demo.entity.Module;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.service.CourseService;
import com.example.demo.service.DashboardService;
import com.example.demo.service.EnrollmentService;
import com.example.demo.service.FileStorageService;
import com.example.demo.service.ModuleService;
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
    private final ModuleService moduleService;
    private final FileStorageService fileStorageService;

    public AdminController(UserService userService,
                           CourseService courseService,
                           EnrollmentService enrollmentService,
                           DashboardService dashboardService,
                           ModuleService moduleService,
                           FileStorageService fileStorageService) {
        this.userService = userService;
        this.courseService = courseService;
        this.enrollmentService = enrollmentService;
        this.dashboardService = dashboardService;
        this.moduleService = moduleService;
        this.fileStorageService = fileStorageService;
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
        List<Course> courses = courseService.findAllCoursesOrdered();
        List<Module> modules = moduleService.findAllModules();
        model.addAttribute("courses", courses);
        model.addAttribute("modules", modules);
        return "admin/courses/list";
    }

    @GetMapping("/courses/new")
    public String newCourseForm(Model model) {
        model.addAttribute("course", new CourseDTO());
        model.addAttribute("modules", moduleService.findActiveModules());
        return "admin/courses/form";
    }

    @PostMapping("/courses/new")
    public String createCourse(@Valid @ModelAttribute("course") CourseDTO courseDTO,
                               BindingResult result,
                               @RequestParam(value = "pdfFile", required = false) MultipartFile pdfFile,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        // Custom validation: either content or PDF must be provided
        boolean hasContent = courseDTO.getContent() != null && !courseDTO.getContent().isBlank();
        boolean hasPdf = pdfFile != null && !pdfFile.isEmpty();
        
        if (!hasContent && !hasPdf) {
            result.rejectValue("content", "error.course", "Either text content or a PDF file is required");
        }
        
        if (result.hasErrors()) {
            model.addAttribute("modules", moduleService.findActiveModules());
            return "admin/courses/form";
        }

        try {
            courseService.createCourse(courseDTO, pdfFile);
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
        model.addAttribute("modules", moduleService.findActiveModules());
        return "admin/courses/form";
    }

    @PostMapping("/courses/{id}/edit")
    public String updateCourse(@PathVariable Long id,
                               @Valid @ModelAttribute("course") CourseDTO courseDTO,
                               BindingResult result,
                               @RequestParam(value = "pdfFile", required = false) MultipartFile pdfFile,
                               @RequestParam(value = "removePdf", required = false, defaultValue = "false") boolean removePdf,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        // Set the removePdf flag
        courseDTO.setRemovePdf(removePdf);
        
        // Get existing course to check current state
        Course existingCourse = courseService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));
        
        // Custom validation: must have either content or PDF after edit
        boolean willHaveContent = courseDTO.getContent() != null && !courseDTO.getContent().isBlank();
        boolean willHavePdf = (existingCourse.hasPdf() && !removePdf) || (pdfFile != null && !pdfFile.isEmpty());
        
        if (!willHaveContent && !willHavePdf) {
            result.rejectValue("content", "error.course", "Either text content or a PDF file is required");
        }
        
        if (result.hasErrors()) {
            model.addAttribute("modules", moduleService.findActiveModules());
            return "admin/courses/form";
        }

        try {
            courseService.updateCourse(id, courseDTO, pdfFile);
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

    @GetMapping("/courses/{id}/pdf")
    public ResponseEntity<Resource> servePdf(@PathVariable Long id) {
        Course course = courseService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));
        
        if (!course.hasPdf()) {
            return ResponseEntity.notFound().build();
        }
        
        Resource resource = fileStorageService.loadFileAsResource(course.getPdfFilename());
        
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "inline; filename=\"" + course.getPdfOriginalName() + "\"")
                .body(resource);
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

    // ========== Module Management ==========

    @GetMapping("/modules")
    public String listModules(Model model) {
        List<Module> modules = moduleService.findAllModules();
        model.addAttribute("modules", modules);
        return "admin/modules/list";
    }

    @GetMapping("/modules/new")
    public String newModuleForm(Model model) {
        ModuleDTO moduleDTO = new ModuleDTO();
        moduleDTO.setActive(true);
        model.addAttribute("module", moduleDTO);
        return "admin/modules/form";
    }

    @PostMapping("/modules/new")
    public String createModule(@Valid @ModelAttribute("module") ModuleDTO moduleDTO,
                               BindingResult result,
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/modules/form";
        }

        try {
            moduleService.createModule(moduleDTO);
            redirectAttributes.addFlashAttribute("success", "Module created successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/modules/new";
        }

        return "redirect:/admin/modules";
    }

    @GetMapping("/modules/{id}")
    public String viewModule(@PathVariable Long id, Model model) {
        Module module = moduleService.findByIdWithCourses(id)
                .orElseThrow(() -> new IllegalArgumentException("Module not found"));
        model.addAttribute("module", module);
        return "admin/modules/view";
    }

    @GetMapping("/modules/{id}/edit")
    public String editModuleForm(@PathVariable Long id, Model model) {
        Module module = moduleService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Module not found"));
        model.addAttribute("module", moduleService.toDTO(module));
        return "admin/modules/form";
    }

    @PostMapping("/modules/{id}/edit")
    public String updateModule(@PathVariable Long id,
                               @Valid @ModelAttribute("module") ModuleDTO moduleDTO,
                               BindingResult result,
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/modules/form";
        }

        try {
            moduleService.updateModule(id, moduleDTO);
            redirectAttributes.addFlashAttribute("success", "Module updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/modules/" + id + "/edit";
        }

        return "redirect:/admin/modules";
    }

    @PostMapping("/modules/{id}/delete")
    public String deleteModule(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            moduleService.deleteModule(id);
            redirectAttributes.addFlashAttribute("success", "Module deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete module: " + e.getMessage());
        }
        return "redirect:/admin/modules";
    }
}
