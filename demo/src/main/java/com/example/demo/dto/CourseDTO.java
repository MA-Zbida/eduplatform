package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for creating and updating courses.
 */
public class CourseDTO {

    private Long id;

    @NotBlank(message = "Course title is required")
    @Size(min = 3, max = 200, message = "Title must be between 3 and 200 characters")
    private String title;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    private String content;

    private String status;

    private boolean indexed;

    private Long moduleId;

    private String moduleName;

    private Integer displayOrder = 0;

    // PDF-related fields
    private String contentType = "TEXT";
    private String pdfFilename;
    private String pdfOriginalName;
    private boolean removePdf = false;

    // Constructors
    public CourseDTO() {}

    public CourseDTO(Long id, String title, String description, String content, String status, boolean indexed) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.content = content;
        this.status = status;
        this.indexed = indexed;
    }

    public CourseDTO(Long id, String title, String description, String content, String status, boolean indexed,
                     Long moduleId, String moduleName, Integer displayOrder) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.content = content;
        this.status = status;
        this.indexed = indexed;
        this.moduleId = moduleId;
        this.moduleName = moduleName;
        this.displayOrder = displayOrder;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isIndexed() {
        return indexed;
    }

    public void setIndexed(boolean indexed) {
        this.indexed = indexed;
    }

    public Long getModuleId() {
        return moduleId;
    }

    public void setModuleId(Long moduleId) {
        this.moduleId = moduleId;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    // PDF-related getters and setters
    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getPdfFilename() {
        return pdfFilename;
    }

    public void setPdfFilename(String pdfFilename) {
        this.pdfFilename = pdfFilename;
    }

    public String getPdfOriginalName() {
        return pdfOriginalName;
    }

    public void setPdfOriginalName(String pdfOriginalName) {
        this.pdfOriginalName = pdfOriginalName;
    }

    public boolean isRemovePdf() {
        return removePdf;
    }

    public void setRemovePdf(boolean removePdf) {
        this.removePdf = removePdf;
    }
}
