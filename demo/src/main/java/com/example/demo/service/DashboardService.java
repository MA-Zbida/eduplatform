package com.example.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.DashboardStatsDTO;
import com.example.demo.entity.CourseStatus;
import com.example.demo.entity.Role;
import com.example.demo.repository.CourseRepository;
import com.example.demo.repository.EnrollmentRepository;
import com.example.demo.repository.QuizRepository;
import com.example.demo.repository.QuizResultRepository;
import com.example.demo.repository.UserRepository;

/**
 * Service class for generating dashboard statistics.
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final QuizRepository quizRepository;
    private final QuizResultRepository quizResultRepository;

    public DashboardService(UserRepository userRepository,
                            CourseRepository courseRepository,
                            EnrollmentRepository enrollmentRepository,
                            QuizRepository quizRepository,
                            QuizResultRepository quizResultRepository) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.quizRepository = quizRepository;
        this.quizResultRepository = quizResultRepository;
    }

    /**
     * Get statistics for the admin dashboard.
     */
    public DashboardStatsDTO getAdminDashboardStats() {
        long totalCourses = courseRepository.count();
        long publishedCourses = courseRepository.countByStatus(CourseStatus.PUBLISHED);
        long totalStudents = userRepository.countByRole(Role.STUDENT);
        long totalQuizzes = quizRepository.count();
        long totalEnrollments = enrollmentRepository.count();

        return new DashboardStatsDTO(
                totalCourses,
                publishedCourses,
                totalStudents,
                totalQuizzes,
                totalEnrollments,
                null
        );
    }

    /**
     * Get statistics for a student's dashboard.
     */
    public DashboardStatsDTO getStudentDashboardStats(Long studentId) {
        long enrolledCourses = enrollmentRepository.findByStudentId(studentId).size();
        long completedCourses = enrollmentRepository.countByStudentIdAndStatus(studentId, 
                com.example.demo.entity.EnrollmentStatus.COMPLETED);
        long totalQuizzes = quizRepository.countByStudentId(studentId);
        long passedQuizzes = quizResultRepository.countPassedByStudentId(studentId);
        Double averageScore = quizResultRepository.getAverageScoreByStudentId(studentId);

        return new DashboardStatsDTO(
                enrolledCourses,
                completedCourses,
                0,
                totalQuizzes,
                passedQuizzes,
                averageScore != null ? averageScore : 0.0
        );
    }
}
