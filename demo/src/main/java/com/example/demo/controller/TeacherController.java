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
import com.example.demo.service.RAGService;
import com.example.demo.service.UserService;

import jakarta.validation.Valid;

/**
 * Controller for teacher functionality.
 * Teachers can manage courses, students, modules, and content.
 * All endpoints require TEACHER role.
 */
@Controller
@RequestMapping("/teacher")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherController {

    private final UserService userService;
    private final CourseService courseService;
    private final EnrollmentService enrollmentService;
    private final DashboardService dashboardService;
    private final ModuleService moduleService;
    private final FileStorageService fileStorageService;
    private final RAGService ragService;
    private final com.example.demo.security.SecurityUtils securityUtils;

    public TeacherController(UserService userService,
                           CourseService courseService,
                           EnrollmentService enrollmentService,
                           DashboardService dashboardService,
                           ModuleService moduleService,
                           FileStorageService fileStorageService,
                           RAGService ragService,
                           com.example.demo.security.SecurityUtils securityUtils) {
        this.userService = userService;
        this.courseService = courseService;
        this.enrollmentService = enrollmentService;
        this.dashboardService = dashboardService;
        this.moduleService = moduleService;
        this.fileStorageService = fileStorageService;
        this.ragService = ragService;
        this.securityUtils = securityUtils;
    }

    // ========== Dashboard ==========

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // Only show stats relevant to the teacher (though dashboardService might need update, likely shared stats are fine for now)
        DashboardStatsDTO stats = dashboardService.getAdminDashboardStats();
        // Show only teacher's courses
        Long teacherId = securityUtils.getCurrentUserId();
        model.addAttribute("stats", stats);
        model.addAttribute("recentCourses", courseService.findByTeacher(teacherId).stream().limit(5).toList());
        model.addAttribute("recentStudents", userService.findAllStudents().stream().limit(5).toList());
        return "teacher/dashboard";
    }

    // ========== Student Management (View Only or Managed?) ==========
    // Teachers can view all students for now, or could restrict to enrolled. 
    // Keeping as is based on "Teacher: Assumes previous ADMINISTRATOR responsibilities"
    @GetMapping("/students")
    public String listStudents(Model model) {
        List<User> students = userService.findAllStudents();
        model.addAttribute("students", students);
        return "teacher/students/list";
    }

    @GetMapping("/students/new")
    public String newStudentForm(Model model) {
        UserDTO userDTO = new UserDTO();
        userDTO.setRole(Role.STUDENT);
        userDTO.setEnabled(true);
        model.addAttribute("student", userDTO);
        return "teacher/students/form";
    }

    @PostMapping("/students/new")
    public String createStudent(@Valid @ModelAttribute("student") UserDTO userDTO,
                                BindingResult result,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "teacher/students/form";
        }

        try {
            userService.createStudent(userDTO);
            redirectAttributes.addFlashAttribute("success", "Student created successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/teacher/students/new";
        }

        return "redirect:/teacher/students";
    }

    @GetMapping("/students/{id}/edit")
    public String editStudentForm(@PathVariable Long id, Model model) {
        User student = userService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        model.addAttribute("student", userService.toDTO(student));
        return "teacher/students/form";
    }

    @PostMapping("/students/{id}/edit")
    public String updateStudent(@PathVariable Long id,
                                @Valid @ModelAttribute("student") UserDTO userDTO,
                                BindingResult result,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "teacher/students/form";
        }

        try {
            userService.updateUser(id, userDTO);
            redirectAttributes.addFlashAttribute("success", "Student updated successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/teacher/students/" + id + "/edit";
        }

        return "redirect:/teacher/students";
    }

    @PostMapping("/students/{id}/delete")
    public String deleteStudent(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("success", "Student deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete student: " + e.getMessage());
        }
        return "redirect:/teacher/students";
    }

    // ========== Course Management ==========

    @GetMapping("/courses")
    public String listCourses(Model model) {
        Long teacherId = securityUtils.getCurrentUserId();
        List<Course> courses = courseService.findByTeacher(teacherId);
        List<Module> modules = moduleService.findByTeacher(teacherId);
        model.addAttribute("courses", courses);
        model.addAttribute("modules", modules);
        return "teacher/courses/list";
    }

    @GetMapping("/courses/new")
    public String newCourseForm(Model model) {
        Long teacherId = securityUtils.getCurrentUserId();
        model.addAttribute("course", new CourseDTO());
        model.addAttribute("modules", moduleService.findActiveByTeacher(teacherId));
        return "teacher/courses/form";
    }

    @PostMapping("/courses/new")
    public String createCourse(@Valid @ModelAttribute("course") CourseDTO courseDTO,
                               BindingResult result,
                               @RequestParam(value = "pdfFile", required = false) MultipartFile pdfFile,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        boolean hasContent = courseDTO.getContent() != null && !courseDTO.getContent().isBlank();
        boolean hasPdf = pdfFile != null && !pdfFile.isEmpty();
        
        if (!hasContent && !hasPdf) {
            result.rejectValue("content", "error.course", "Either text content or a PDF file is required");
        }
        
        if (result.hasErrors()) {
            Long teacherId = securityUtils.getCurrentUserId();
            model.addAttribute("modules", moduleService.findActiveByTeacher(teacherId));
            return "teacher/courses/form";
        }

        try {
            courseService.createCourse(courseDTO, pdfFile);
            redirectAttributes.addFlashAttribute("success", "Course created successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/teacher/courses/new";
        }

        return "redirect:/teacher/courses";
    }

    @GetMapping("/courses/{id}")
    public String viewCourse(@PathVariable Long id, Model model) {
        Long teacherId = securityUtils.getCurrentUserId();
        Course course = courseService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));
        
        // Verify ownership
        if (!course.getCreatedBy().getId().equals(teacherId)) {
            throw new org.springframework.security.access.AccessDeniedException("You do not own this course");
        }

        List<Enrollment> enrollments = enrollmentService.findByCourse(id);
        List<User> availableStudents = userService.findStudentsNotEnrolledInCourse(id);

        model.addAttribute("course", course);
        model.addAttribute("enrollments", enrollments);
        model.addAttribute("availableStudents", availableStudents);
        return "teacher/courses/view";
    }

    @GetMapping("/courses/{id}/edit")
    public String editCourseForm(@PathVariable Long id, Model model) {
        Long teacherId = securityUtils.getCurrentUserId();
        Course course = courseService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));
        
        if (!course.getCreatedBy().getId().equals(teacherId)) {
            throw new org.springframework.security.access.AccessDeniedException("You do not own this course");
        }

        model.addAttribute("course", courseService.toDTO(course));
        model.addAttribute("modules", moduleService.findActiveByTeacher(teacherId));
        return "teacher/courses/form";
    }

    @PostMapping("/courses/{id}/edit")
    public String updateCourse(@PathVariable Long id,
                               @Valid @ModelAttribute("course") CourseDTO courseDTO,
                               BindingResult result,
                               @RequestParam(value = "pdfFile", required = false) MultipartFile pdfFile,
                               @RequestParam(value = "removePdf", required = false, defaultValue = "false") boolean removePdf,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        Long teacherId = securityUtils.getCurrentUserId();
        Course existingCourse = courseService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));
        
        if (!existingCourse.getCreatedBy().getId().equals(teacherId)) {
            throw new org.springframework.security.access.AccessDeniedException("You do not own this course");
        }

        courseDTO.setRemovePdf(removePdf);
        
        boolean willHaveContent = courseDTO.getContent() != null && !courseDTO.getContent().isBlank();
        boolean willHavePdf = (existingCourse.hasPdf() && !removePdf) || (pdfFile != null && !pdfFile.isEmpty());
        
        if (!willHaveContent && !willHavePdf) {
            result.rejectValue("content", "error.course", "Either text content or a PDF file is required");
        }
        
        if (result.hasErrors()) {
            model.addAttribute("modules", moduleService.findActiveByTeacher(teacherId));
            return "teacher/courses/form";
        }

        try {
            courseService.updateCourse(id, courseDTO, pdfFile);
            redirectAttributes.addFlashAttribute("success", "Course updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/teacher/courses/" + id + "/edit";
        }

        return "redirect:/teacher/courses";
    }

    @PostMapping("/courses/{id}/delete")
    public String deleteCourse(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Long teacherId = securityUtils.getCurrentUserId();
        Course course = courseService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));
        
        if (!course.getCreatedBy().getId().equals(teacherId)) {
             redirectAttributes.addFlashAttribute("error", "You do not own this course");
             return "redirect:/teacher/courses";
        }

        try {
            courseService.deleteCourse(id);
            redirectAttributes.addFlashAttribute("success", "Course deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete course: " + e.getMessage());
        }
        return "redirect:/teacher/courses";
    }

    @GetMapping("/courses/{id}/pdf")
    public ResponseEntity<Resource> servePdf(@PathVariable Long id) {
        Long teacherId = securityUtils.getCurrentUserId();
        Course course = courseService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));

        if (!course.getCreatedBy().getId().equals(teacherId)) {
            return ResponseEntity.status(403).build();
        }
        
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
        Long teacherId = securityUtils.getCurrentUserId();
        Course course = courseService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));
        
        if (!course.getCreatedBy().getId().equals(teacherId)) {
             redirectAttributes.addFlashAttribute("error", "You do not own this course");
             return "redirect:/teacher/courses";
        }

        try {
            courseService.publishCourse(id);
            redirectAttributes.addFlashAttribute("success", "Course published successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/teacher/courses/" + id;
    }

    @PostMapping("/courses/{id}/index")
    public String indexCourse(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Long teacherId = securityUtils.getCurrentUserId();
        Course course = courseService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));
                
        if (!course.getCreatedBy().getId().equals(teacherId)) {
             redirectAttributes.addFlashAttribute("error", "You do not own this course");
             return "redirect:/teacher/courses";
        }

        try {
            courseService.indexCourseForRAG(id);
            redirectAttributes.addFlashAttribute("success", "Course indexed for AI quiz generation!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/teacher/courses/" + id;
    }

    @GetMapping("/courses/{id}/rag")
    public String viewRAGOutput(@PathVariable Long id, Model model) {
        Long teacherId = securityUtils.getCurrentUserId();
        Course course = courseService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));

        if (!course.getCreatedBy().getId().equals(teacherId)) {
            throw new org.springframework.security.access.AccessDeniedException("You do not own this course");
        }
        
        if (!course.isIndexed()) {
            throw new IllegalStateException("Course has not been indexed for RAG yet.");
        }
        
        var chunks = ragService.retrieveChunks(id);
        var stats = ragService.getRAGStats(id);
        
        model.addAttribute("course", course);
        model.addAttribute("chunks", chunks);
        model.addAttribute("stats", stats);
        
        return "teacher/courses/rag";
    }

    // ========== Enrollment Management ==========

    @PostMapping("/courses/{courseId}/enroll")
    public String enrollStudent(@PathVariable Long courseId,
                                @RequestParam Long studentId,
                                RedirectAttributes redirectAttributes) {
        Long teacherId = securityUtils.getCurrentUserId();
        Course course = courseService.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));
        
        if (!course.getCreatedBy().getId().equals(teacherId)) {
             redirectAttributes.addFlashAttribute("error", "You do not own this course");
             return "redirect:/teacher/courses/" + courseId;
        }

        try {
            enrollmentService.enrollStudent(studentId, courseId);
            redirectAttributes.addFlashAttribute("success", "Student enrolled successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/teacher/courses/" + courseId;
    }

    @PostMapping("/courses/{courseId}/unenroll/{studentId}")
    public String unenrollStudent(@PathVariable Long courseId,
                                  @PathVariable Long studentId,
                                  RedirectAttributes redirectAttributes) {
        Long teacherId = securityUtils.getCurrentUserId();
        Course course = courseService.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));
        
        if (!course.getCreatedBy().getId().equals(teacherId)) {
             redirectAttributes.addFlashAttribute("error", "You do not own this course");
             return "redirect:/teacher/courses/" + courseId;
        }

        try {
            enrollmentService.unenrollStudent(studentId, courseId);
            redirectAttributes.addFlashAttribute("success", "Student unenrolled successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/teacher/courses/" + courseId;
    }

    // ========== Module Management ==========

    @GetMapping("/modules")
    public String listModules(Model model) {
        Long teacherId = securityUtils.getCurrentUserId();
        List<Module> modules = moduleService.findByTeacher(teacherId);
        model.addAttribute("modules", modules);
        return "teacher/modules/list";
    }

    @GetMapping("/modules/new")
    public String newModuleForm(Model model) {
        ModuleDTO moduleDTO = new ModuleDTO();
        moduleDTO.setActive(true);
        model.addAttribute("module", moduleDTO);
        return "teacher/modules/form";
    }

    @PostMapping("/modules/new")
    public String createModule(@Valid @ModelAttribute("module") ModuleDTO moduleDTO,
                               BindingResult result,
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "teacher/modules/form";
        }

        try {
            moduleService.createModule(moduleDTO);
            redirectAttributes.addFlashAttribute("success", "Module created successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/teacher/modules/new";
        }

        return "redirect:/teacher/modules";
    }

    @GetMapping("/modules/{id}")
    public String viewModule(@PathVariable Long id, Model model) {
        Long teacherId = securityUtils.getCurrentUserId();
        Module module = moduleService.findByIdWithCourses(id)
                .orElseThrow(() -> new IllegalArgumentException("Module not found"));
        
        if (!module.getCreatedBy().getId().equals(teacherId)) {
             throw new org.springframework.security.access.AccessDeniedException("You do not own this module");
        }

        model.addAttribute("module", module);
        return "teacher/modules/view";
    }

    @GetMapping("/modules/{id}/edit")
    public String editModuleForm(@PathVariable Long id, Model model) {
        Long teacherId = securityUtils.getCurrentUserId();
        Module module = moduleService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Module not found"));
        
        if (!module.getCreatedBy().getId().equals(teacherId)) {
             throw new org.springframework.security.access.AccessDeniedException("You do not own this module");
        }

        model.addAttribute("module", moduleService.toDTO(module));
        return "teacher/modules/form";
    }

    @PostMapping("/modules/{id}/edit")
    public String updateModule(@PathVariable Long id,
                               @Valid @ModelAttribute("module") ModuleDTO moduleDTO,
                               BindingResult result,
                               RedirectAttributes redirectAttributes) {
        Long teacherId = securityUtils.getCurrentUserId();
        Module module = moduleService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Module not found"));
                
        if (!module.getCreatedBy().getId().equals(teacherId)) {
             redirectAttributes.addFlashAttribute("error", "You do not own this module");
             return "redirect:/teacher/modules";
        }
        
        if (result.hasErrors()) {
            return "teacher/modules/form";
        }

        try {
            moduleService.updateModule(id, moduleDTO);
            redirectAttributes.addFlashAttribute("success", "Module updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/teacher/modules/" + id + "/edit";
        }

        return "redirect:/teacher/modules";
    }

    @PostMapping("/modules/{id}/delete")
    public String deleteModule(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Long teacherId = securityUtils.getCurrentUserId();
        Module module = moduleService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Module not found"));
        
        if (!module.getCreatedBy().getId().equals(teacherId)) {
             redirectAttributes.addFlashAttribute("error", "You do not own this module");
             return "redirect:/teacher/modules";
        }

        try {
            moduleService.deleteModule(id);
            redirectAttributes.addFlashAttribute("success", "Module deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete module: " + e.getMessage());
        }
        return "redirect:/teacher/modules";
    }
}
