package com.example.demo.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.CourseDTO;
import com.example.demo.entity.ContentType;
import com.example.demo.entity.Course;
import com.example.demo.entity.CourseStatus;
import com.example.demo.entity.Module;
import com.example.demo.entity.User;
import com.example.demo.repository.CourseRepository;
import com.example.demo.repository.EnrollmentRepository;
import com.example.demo.repository.ModuleRepository;
import com.example.demo.security.SecurityUtils;

/**
 * Service class for Course entity operations.
 * Handles course management including creation, publishing, and retrieval.
 */
@Service
@Transactional
public class CourseService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ModuleRepository moduleRepository;
    private final SecurityUtils securityUtils;
    private final RAGService ragService;
    private final FileStorageService fileStorageService;

    public CourseService(CourseRepository courseRepository, 
                         EnrollmentRepository enrollmentRepository,
                         ModuleRepository moduleRepository,
                         SecurityUtils securityUtils,
                         RAGService ragService,
                         FileStorageService fileStorageService) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.moduleRepository = moduleRepository;
        this.securityUtils = securityUtils;
        this.ragService = ragService;
        this.fileStorageService = fileStorageService;
    }

    public Course createCourse(CourseDTO dto) {
        return createCourse(dto, null);
    }

    public Course createCourse(CourseDTO dto, MultipartFile pdfFile) {
        User currentUser = securityUtils.getCurrentUser();
        if (currentUser == null || (!currentUser.isAdmin() && !currentUser.isTeacher())) {
            throw new SecurityException("Only administrators or teachers can create courses");
        }

        Course course = new Course();
        course.setTitle(dto.getTitle());
        course.setDescription(dto.getDescription());
        course.setCreatedBy(currentUser);
        course.setStatus(CourseStatus.DRAFT);
        course.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0);

        // Handle content type
        if (pdfFile != null && !pdfFile.isEmpty()) {
            // PDF upload
            String storedFilename = fileStorageService.storeFile(pdfFile);
            course.setPdfFilename(storedFilename);
            course.setPdfOriginalName(pdfFile.getOriginalFilename());
            course.setContentType(ContentType.PDF);
            // Set a placeholder content for PDF courses (required by entity constraint)
            course.setContent("[PDF Content: " + pdfFile.getOriginalFilename() + "]");
        } else {
            // Text content
            course.setContent(dto.getContent() != null ? dto.getContent() : "");
            course.setContentType(ContentType.TEXT);
        }

        // Set module if provided
        if (dto.getModuleId() != null) {
            Module module = moduleRepository.findById(dto.getModuleId())
                    .orElseThrow(() -> new IllegalArgumentException("Module not found: " + dto.getModuleId()));
            course.setModule(module);
        }

        return courseRepository.save(course);
    }

    public Course updateCourse(Long id, CourseDTO dto) {
        return updateCourse(id, dto, null);
    }

    public Course updateCourse(Long id, CourseDTO dto, MultipartFile pdfFile) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + id));

        course.setTitle(dto.getTitle());
        course.setDescription(dto.getDescription());
        course.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0);

        // Handle PDF removal request
        if (dto.isRemovePdf() && course.hasPdf()) {
            fileStorageService.deleteFile(course.getPdfFilename());
            course.setPdfFilename(null);
            course.setPdfOriginalName(null);
            course.setContentType(ContentType.TEXT);
            course.setContent(dto.getContent() != null ? dto.getContent() : "");
        }
        // Handle new PDF upload
        else if (pdfFile != null && !pdfFile.isEmpty()) {
            // Delete old PDF if exists
            if (course.hasPdf()) {
                fileStorageService.deleteFile(course.getPdfFilename());
            }
            String storedFilename = fileStorageService.storeFile(pdfFile);
            course.setPdfFilename(storedFilename);
            course.setPdfOriginalName(pdfFile.getOriginalFilename());
            course.setContentType(ContentType.PDF);
            course.setContent("[PDF Content: " + pdfFile.getOriginalFilename() + "]");
        }
        // Update text content (only if not PDF type)
        else if (course.getContentType() == ContentType.TEXT) {
            course.setContent(dto.getContent() != null ? dto.getContent() : "");
        }

        // Update module
        if (dto.getModuleId() != null) {
            Module module = moduleRepository.findById(dto.getModuleId())
                    .orElseThrow(() -> new IllegalArgumentException("Module not found: " + dto.getModuleId()));
            course.setModule(module);
        } else {
            course.setModule(null);
        }

        // If content changed, mark as not indexed
        if (course.isIndexed()) {
            course.setIndexed(false);
        }

        return courseRepository.save(course);
    }

    public void deleteCourse(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + id));
        
        // Delete PDF file if exists
        if (course.hasPdf()) {
            fileStorageService.deleteFile(course.getPdfFilename());
        }
        
        courseRepository.delete(course);
    }

    public Course publishCourse(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + id));

        if (!course.canBePublished()) {
            throw new IllegalStateException("Course cannot be published. Ensure it has content and is in DRAFT status.");
        }

        course.publish();
        return courseRepository.save(course);
    }

    public Course indexCourseForRAG(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + id));

        if (!course.isPublished()) {
            throw new IllegalStateException("Only published courses can be indexed for RAG.");
        }

        // Perform RAG indexing
        ragService.indexCourse(course);
        course.markAsIndexed();

        return courseRepository.save(course);
    }

    @Transactional(readOnly = true)
    public Optional<Course> findById(Long id) {
        return courseRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Course> findAllCourses() {
        return courseRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Course> findPublishedCourses() {
        return courseRepository.findByStatus(CourseStatus.PUBLISHED);
    }

    @Transactional(readOnly = true)
    public List<Course> findEnrolledCourses(Long studentId) {
        return courseRepository.findEnrolledCoursesByStudentId(studentId, CourseStatus.PUBLISHED);
    }

    @Transactional(readOnly = true)
    public List<Course> findByModule(Long moduleId) {
        return courseRepository.findByModuleIdOrderByDisplayOrderAscTitleAsc(moduleId);
    }

    @Transactional(readOnly = true)
    public List<Course> findPublishedByModule(Long moduleId) {
        return courseRepository.findByModuleIdAndStatusOrderByDisplayOrderAscTitleAsc(moduleId, CourseStatus.PUBLISHED);
    }

    @Transactional(readOnly = true)
    public List<Course> findCoursesWithoutModule() {
        return courseRepository.findCoursesWithoutModule();
    }

    @Transactional(readOnly = true)
    public List<Course> findAllCoursesOrdered() {
        return courseRepository.findAllByOrderByModuleIdAscDisplayOrderAscTitleAsc();
    }

    @Transactional(readOnly = true)
    public List<Course> findByTeacher(Long teacherId) {
        return courseRepository.findByCreatedByIdOrderByModuleIdAscDisplayOrderAscTitleAsc(teacherId);
    }

    @Transactional(readOnly = true)
    public boolean isStudentEnrolled(Long studentId, Long courseId) {
        return enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId);
    }

    @Transactional(readOnly = true)
    public long countAllCourses() {
        return courseRepository.count();
    }

    @Transactional(readOnly = true)
    public long countPublishedCourses() {
        return courseRepository.countByStatus(CourseStatus.PUBLISHED);
    }

    public CourseDTO toDTO(Course course) {
        CourseDTO dto = new CourseDTO(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getContent(),
                course.getStatus().name(),
                course.isIndexed(),
                course.getModule() != null ? course.getModule().getId() : null,
                course.getModule() != null ? course.getModule().getName() : null,
                course.getDisplayOrder()
        );
        dto.setContentType(course.getContentType().name());
        dto.setPdfFilename(course.getPdfFilename());
        dto.setPdfOriginalName(course.getPdfOriginalName());
        return dto;
    }
}
