package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.CourseChunk;

/**
 * Repository for CourseChunk entity operations.
 * Used by the RAG system to retrieve relevant course content chunks.
 */
@Repository
public interface CourseChunkRepository extends JpaRepository<CourseChunk, Long> {
    
    List<CourseChunk> findByCourseIdOrderByChunkIndexAsc(Long courseId);
    
    @Query("SELECT cc FROM CourseChunk cc WHERE cc.course.id = :courseId ORDER BY cc.chunkIndex")
    List<CourseChunk> findAllChunksByCourseId(@Param("courseId") Long courseId);
    
    @Modifying
    @Query("DELETE FROM CourseChunk cc WHERE cc.course.id = :courseId")
    void deleteByCourseId(@Param("courseId") Long courseId);
    
    @Query("SELECT COUNT(cc) FROM CourseChunk cc WHERE cc.course.id = :courseId")
    long countByCourseId(@Param("courseId") Long courseId);
    
    @Query("SELECT cc FROM CourseChunk cc WHERE cc.course.id = :courseId AND " +
           "LOWER(cc.content) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<CourseChunk> findByKeyword(@Param("courseId") Long courseId, @Param("keyword") String keyword);
}
