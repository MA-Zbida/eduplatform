package com.example.demo.entity;

/**
 * Enumeration representing user roles in the platform.
 * Three roles exist in hierarchy: ADMINISTRATOR (super admin) > TEACHER > STUDENT.
 * 
 * ADMINISTRATOR: Platform supervision, teacher management, activity monitoring
 * TEACHER: Course management, student management, content creation
 * STUDENT: Learning, taking quizzes, viewing courses
 */
public enum Role {
    ADMINISTRATOR,
    TEACHER,
    STUDENT
}
