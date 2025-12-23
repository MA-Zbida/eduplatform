package com.example.demo.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Course;
import com.example.demo.entity.Enrollment;
import com.example.demo.entity.EnrollmentStatus;
import com.example.demo.entity.User;
import com.example.demo.repository.CourseRepository;
import com.example.demo.repository.EnrollmentRepository;
import com.example.demo.repository.UserRepository;

/**
 * Service class for Enrollment entity operations.
 * Handles student enrollment to courses.
 */
@Service
@Transactional
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    public EnrollmentService(EnrollmentRepository enrollmentRepository,
                             UserRepository userRepository,
                             CourseRepository courseRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
    }

    public Enrollment enrollStudent(Long studentId, Long courseId) {
        // Verify student exists and is a student
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        if (!student.isStudent()) {
            throw new IllegalArgumentException("User is not a student: " + studentId);
        }

        // Verify course exists
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));

        // Check if already enrolled
        if (enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
            throw new IllegalStateException("Student is already enrolled in this course");
        }

        Enrollment enrollment = new Enrollment(student, course);
        return enrollmentRepository.save(enrollment);
    }

    public void unenrollStudent(Long studentId, Long courseId) {
        Enrollment enrollment = enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found"));
        enrollmentRepository.delete(enrollment);
    }

    public Enrollment markAsCompleted(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));
        enrollment.markAsCompleted();
        return enrollmentRepository.save(enrollment);
    }

    public Enrollment updateProgress(Long enrollmentId, int progressPercentage) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));
        enrollment.updateProgress(progressPercentage);
        return enrollmentRepository.save(enrollment);
    }

    @Transactional(readOnly = true)
    public Optional<Enrollment> findById(Long id) {
        return enrollmentRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Enrollment> findByStudentAndCourse(Long studentId, Long courseId) {
        return enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId);
    }

    @Transactional(readOnly = true)
    public List<Enrollment> findByStudent(Long studentId) {
        return enrollmentRepository.findByStudentId(studentId);
    }

    @Transactional(readOnly = true)
    public List<Enrollment> findByCourse(Long courseId) {
        return enrollmentRepository.findByCourseId(courseId);
    }

    @Transactional(readOnly = true)
    public boolean isEnrolled(Long studentId, Long courseId) {
        return enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId);
    }

    @Transactional(readOnly = true)
    public long countEnrollments() {
        return enrollmentRepository.count();
    }

    @Transactional(readOnly = true)
    public long countCompletedByStudent(Long studentId) {
        return enrollmentRepository.countByStudentIdAndStatus(studentId, EnrollmentStatus.COMPLETED);
    }

    /**
     * Mark a course as learned/completed by the student.
     * This allows the student to take quizzes on this specific course.
     */
    public Enrollment markCourseAsLearned(Long studentId, Long courseId) {
        Enrollment enrollment = enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found for student: " + studentId + " and course: " + courseId));
        enrollment.markCourseAsLearned();
        return enrollmentRepository.save(enrollment);
    }

    /**
     * Check if a student has completed learning a specific course.
     */
    @Transactional(readOnly = true)
    public boolean hasCourseCompleted(Long studentId, Long courseId) {
        return enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId)
                .map(Enrollment::isCourseCompleted)
                .orElse(false);
    }

    /**
     * Get all courses that a student has marked as learned.
     */
    @Transactional(readOnly = true)
    public List<Enrollment> findCompletedCoursesByStudent(Long studentId) {
        return enrollmentRepository.findCompletedCoursesByStudentId(studentId);
    }

    /**
     * Get enrollments for a student in a specific module.
     */
    @Transactional(readOnly = true)
    public List<Enrollment> findByStudentAndModule(Long studentId, Long moduleId) {
        return enrollmentRepository.findByStudentIdAndModuleId(studentId, moduleId);
    }

    /**
     * Get completed courses for a student in a specific module.
     */
    @Transactional(readOnly = true)
    public List<Enrollment> findCompletedByStudentAndModule(Long studentId, Long moduleId) {
        return enrollmentRepository.findCompletedByStudentIdAndModuleId(studentId, moduleId);
    }

    /**
     * Count completed courses for a student in a specific module.
     */
    @Transactional(readOnly = true)
    public long countCompletedByStudentAndModule(Long studentId, Long moduleId) {
        return enrollmentRepository.countCompletedByStudentIdAndModuleId(studentId, moduleId);
    }
}
