package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.Course;
import com.example.demo.entity.CourseStatus;

/**
 * Repository for Course entity operations.
 */
@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    
    List<Course> findByStatus(CourseStatus status);
    
    List<Course> findByCreatedById(Long createdById);
    
    List<Course> findByStatusAndIndexed(CourseStatus status, boolean indexed);
    
    @Query("SELECT c FROM Course c JOIN c.enrollments e WHERE e.student.id = :studentId AND c.status = :status")
    List<Course> findEnrolledCoursesByStudentId(@Param("studentId") Long studentId, @Param("status") CourseStatus status);
    
    @Query("SELECT c FROM Course c WHERE c.status = 'PUBLISHED' AND c.indexed = true")
    List<Course> findPublishedAndIndexedCourses();
    
    @Query("SELECT COUNT(c) FROM Course c WHERE c.status = :status")
    long countByStatus(@Param("status") CourseStatus status);
    
    boolean existsByTitleIgnoreCase(String title);

    List<Course> findByModuleIdOrderByDisplayOrderAscTitleAsc(Long moduleId);

    List<Course> findByModuleIdAndStatusOrderByDisplayOrderAscTitleAsc(Long moduleId, CourseStatus status);

    @Query("SELECT c FROM Course c WHERE c.module.id = :moduleId AND c.status = 'PUBLISHED' AND c.indexed = true ORDER BY c.displayOrder ASC")
    List<Course> findPublishedAndIndexedByModuleId(@Param("moduleId") Long moduleId);

    @Query("SELECT c FROM Course c WHERE c.module IS NULL ORDER BY c.title ASC")
    List<Course> findCoursesWithoutModule();

    List<Course> findAllByOrderByModuleIdAscDisplayOrderAscTitleAsc();
}
