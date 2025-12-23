package com.example.demo.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.ModuleDTO;
import com.example.demo.entity.Module;
import com.example.demo.entity.User;
import com.example.demo.repository.ModuleRepository;
import com.example.demo.security.SecurityUtils;

/**
 * Service class for Module entity operations.
 * Handles module management including creation, update, and retrieval.
 */
@Service
@Transactional
public class ModuleService {

    private final ModuleRepository moduleRepository;
    private final SecurityUtils securityUtils;

    public ModuleService(ModuleRepository moduleRepository, SecurityUtils securityUtils) {
        this.moduleRepository = moduleRepository;
        this.securityUtils = securityUtils;
    }

    public Module createModule(ModuleDTO dto) {
        User currentUser = securityUtils.getCurrentUser();
        if (currentUser == null || !currentUser.isAdmin()) {
            throw new SecurityException("Only administrators can create modules");
        }

        if (moduleRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("A module with this name already exists");
        }

        Module module = new Module();
        module.setName(dto.getName());
        module.setDescription(dto.getDescription());
        module.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0);
        module.setActive(dto.isActive());
        module.setCreatedBy(currentUser);

        return moduleRepository.save(module);
    }

    public Module updateModule(Long id, ModuleDTO dto) {
        Module module = moduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Module not found: " + id));

        // Check for duplicate name (excluding current module)
        Optional<Module> existingModule = moduleRepository.findByName(dto.getName());
        if (existingModule.isPresent() && !existingModule.get().getId().equals(id)) {
            throw new IllegalArgumentException("A module with this name already exists");
        }

        module.setName(dto.getName());
        module.setDescription(dto.getDescription());
        module.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0);
        module.setActive(dto.isActive());

        return moduleRepository.save(module);
    }

    public void deleteModule(Long id) {
        Module module = moduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Module not found: " + id));

        if (!module.getCourses().isEmpty()) {
            throw new IllegalStateException("Cannot delete module with courses. Remove or reassign courses first.");
        }

        moduleRepository.delete(module);
    }

    @Transactional(readOnly = true)
    public Optional<Module> findById(Long id) {
        return moduleRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Module> findByIdWithCourses(Long id) {
        return moduleRepository.findByIdWithCourses(id);
    }

    @Transactional(readOnly = true)
    public List<Module> findAllModules() {
        return moduleRepository.findAllByOrderByDisplayOrderAscNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Module> findActiveModules() {
        return moduleRepository.findByActiveOrderByDisplayOrderAscNameAsc(true);
    }

    @Transactional(readOnly = true)
    public List<Module> findActiveModulesWithCourses() {
        return moduleRepository.findActiveModulesWithCourses();
    }

    @Transactional(readOnly = true)
    public long countModules() {
        return moduleRepository.count();
    }

    public ModuleDTO toDTO(Module module) {
        return new ModuleDTO(
                module.getId(),
                module.getName(),
                module.getDescription(),
                module.getDisplayOrder(),
                module.isActive(),
                module.getCourseCount(),
                module.getPublishedCourseCount()
        );
    }

    public ModuleDTO toDTOWithCounts(Module module) {
        long publishedCount = moduleRepository.countPublishedCoursesByModuleId(module.getId());
        long totalCount = moduleRepository.countCoursesByModuleId(module.getId());
        return new ModuleDTO(
                module.getId(),
                module.getName(),
                module.getDescription(),
                module.getDisplayOrder(),
                module.isActive(),
                (int) totalCount,
                publishedCount
        );
    }
}
