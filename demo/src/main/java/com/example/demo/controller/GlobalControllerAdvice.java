package com.example.demo.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Global controller advice to add common model attributes.
 */
@ControllerAdvice
public class GlobalControllerAdvice {

    /**
     * Adds the current page identifier to all models for sidebar navigation highlighting.
     */
    @ModelAttribute("currentPage")
    public String currentPage(HttpServletRequest request) {
        String uri = request.getRequestURI();
        
        // Admin pages
        if (uri.contains("/admin/dashboard")) {
            return "admin-dashboard";
        } else if (uri.contains("/admin/courses")) {
            return "admin-courses";
        } else if (uri.contains("/admin/students")) {
            return "admin-students";
        }
        
        // Student pages
        if (uri.contains("/student/dashboard")) {
            return "student-dashboard";
        } else if (uri.contains("/student/courses")) {
            return "student-courses";
        } else if (uri.contains("/student/quizzes")) {
            return "student-quizzes";
        } else if (uri.contains("/student/history")) {
            return "student-history";
        }
        
        return "";
    }
}
