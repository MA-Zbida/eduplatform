package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for Module entity.
 */
public class ModuleDTO {

    private Long id;

    @NotBlank(message = "Module name is required")
    @Size(min = 3, max = 200, message = "Name must be between 3 and 200 characters")
    private String name;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    private Integer displayOrder = 0;

    private boolean active = true;

    private int courseCount;

    private long publishedCourseCount;

    // Constructors
    public ModuleDTO() {}

    public ModuleDTO(Long id, String name, String description, Integer displayOrder, boolean active) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.displayOrder = displayOrder;
        this.active = active;
    }

    public ModuleDTO(Long id, String name, String description, Integer displayOrder, boolean active, int courseCount, long publishedCourseCount) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.displayOrder = displayOrder;
        this.active = active;
        this.courseCount = courseCount;
        this.publishedCourseCount = publishedCourseCount;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getCourseCount() {
        return courseCount;
    }

    public void setCourseCount(int courseCount) {
        this.courseCount = courseCount;
    }

    public long getPublishedCourseCount() {
        return publishedCourseCount;
    }

    public void setPublishedCourseCount(long publishedCourseCount) {
        this.publishedCourseCount = publishedCourseCount;
    }
}
