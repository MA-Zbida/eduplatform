package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.Quiz;

/**
 * Repository for Quiz entity operations.
 */
@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {
    
    List<Quiz> findByStudentId(Long studentId);
    
    List<Quiz> findByCourseId(Long courseId);
    
    List<Quiz> findByStudentIdAndCourseId(Long studentId, Long courseId);
    
    @Query("SELECT q FROM Quiz q WHERE q.student.id = :studentId ORDER BY q.createdAt DESC")
    List<Quiz> findByStudentIdOrderByCreatedAtDesc(@Param("studentId") Long studentId);
    
    @Query("SELECT q FROM Quiz q LEFT JOIN FETCH q.questions WHERE q.id = :quizId")
    Quiz findByIdWithQuestions(@Param("quizId") Long quizId);
    
    @Query("SELECT COUNT(q) FROM Quiz q WHERE q.student.id = :studentId")
    long countByStudentId(@Param("studentId") Long studentId);
    
    @Query("SELECT COUNT(q) FROM Quiz q WHERE q.course.id = :courseId")
    long countByCourseId(@Param("courseId") Long courseId);
}
