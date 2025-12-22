package com.example.demo.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.CourseDTO;
import com.example.demo.entity.Course;
import com.example.demo.entity.CourseStatus;
import com.example.demo.entity.User;
import com.example.demo.repository.CourseRepository;
import com.example.demo.repository.EnrollmentRepository;
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
    private final SecurityUtils securityUtils;
    private final RAGService ragService;

    public CourseService(CourseRepository courseRepository, 
                         EnrollmentRepository enrollmentRepository,
                         SecurityUtils securityUtils,
                         RAGService ragService) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.securityUtils = securityUtils;
        this.ragService = ragService;
    }

    public Course createCourse(CourseDTO dto) {
        User currentUser = securityUtils.getCurrentUser();
        if (currentUser == null || !currentUser.isAdmin()) {
            throw new SecurityException("Only administrators can create courses");
        }

        Course course = new Course();
        course.setTitle(dto.getTitle());
        course.setDescription(dto.getDescription());
        course.setContent(dto.getContent());
        course.setCreatedBy(currentUser);
        course.setStatus(CourseStatus.DRAFT);

        return courseRepository.save(course);
    }

    public Course updateCourse(Long id, CourseDTO dto) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + id));

        course.setTitle(dto.getTitle());
        course.setDescription(dto.getDescription());
        course.setContent(dto.getContent());

        // If content changed, mark as not indexed
        if (course.isIndexed()) {
            course.setIndexed(false);
        }

        return courseRepository.save(course);
    }

    public void deleteCourse(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + id));
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
        return new CourseDTO(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getContent(),
                course.getStatus().name(),
                course.isIndexed()
        );
    }
}
