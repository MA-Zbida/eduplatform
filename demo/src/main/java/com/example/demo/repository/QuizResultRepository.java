package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.QuizResult;

/**
 * Repository for QuizResult entity operations.
 */
@Repository
public interface QuizResultRepository extends JpaRepository<QuizResult, Long> {
    
    Optional<QuizResult> findByQuizId(Long quizId);
    
    List<QuizResult> findByStudentId(Long studentId);
    
    @Query("SELECT qr FROM QuizResult qr WHERE qr.student.id = :studentId ORDER BY qr.completedAt DESC")
    List<QuizResult> findByStudentIdOrderByCompletedAtDesc(@Param("studentId") Long studentId);
    
    @Query("SELECT qr FROM QuizResult qr WHERE qr.quiz.course.id = :courseId AND qr.student.id = :studentId ORDER BY qr.completedAt DESC")
    List<QuizResult> findByCourseIdAndStudentId(@Param("courseId") Long courseId, @Param("studentId") Long studentId);
    
    @Query("SELECT AVG(qr.scorePercentage) FROM QuizResult qr WHERE qr.student.id = :studentId")
    Double getAverageScoreByStudentId(@Param("studentId") Long studentId);
    
    @Query("SELECT AVG(qr.scorePercentage) FROM QuizResult qr WHERE qr.quiz.course.id = :courseId")
    Double getAverageScoreByCourseId(@Param("courseId") Long courseId);
    
    @Query("SELECT COUNT(qr) FROM QuizResult qr WHERE qr.student.id = :studentId AND qr.passed = true")
    long countPassedByStudentId(@Param("studentId") Long studentId);
    
    @Query("SELECT COUNT(qr) FROM QuizResult qr WHERE qr.quiz.course.id = :courseId AND qr.student.id = :studentId AND qr.passed = true")
    long countPassedByCourseIdAndStudentId(@Param("courseId") Long courseId, @Param("studentId") Long studentId);
}
