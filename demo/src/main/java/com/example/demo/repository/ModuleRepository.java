package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.Module;

/**
 * Repository for Module entity operations.
 */
@Repository
public interface ModuleRepository extends JpaRepository<Module, Long> {

    List<Module> findByActiveOrderByDisplayOrderAscNameAsc(boolean active);

    List<Module> findAllByOrderByDisplayOrderAscNameAsc();

    List<Module> findByCreatedByIdOrderByDisplayOrderAscNameAsc(Long createdById);

    List<Module> findByCreatedByIdAndActiveOrderByDisplayOrderAscNameAsc(Long createdById, boolean active);

    Optional<Module> findByName(String name);

    boolean existsByName(String name);

    @Query("SELECT m FROM Module m LEFT JOIN FETCH m.courses WHERE m.id = :id")
    Optional<Module> findByIdWithCourses(@Param("id") Long id);

    @Query("SELECT m FROM Module m LEFT JOIN FETCH m.courses c WHERE m.active = true ORDER BY m.displayOrder ASC, m.name ASC")
    List<Module> findActiveModulesWithCourses();

    @Query("SELECT COUNT(c) FROM Course c WHERE c.module.id = :moduleId")
    long countCoursesByModuleId(@Param("moduleId") Long moduleId);

    @Query("SELECT COUNT(c) FROM Course c WHERE c.module.id = :moduleId AND c.status = 'PUBLISHED'")
    long countPublishedCoursesByModuleId(@Param("moduleId") Long moduleId);
}
