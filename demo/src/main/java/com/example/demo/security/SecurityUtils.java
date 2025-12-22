package com.example.demo.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.example.demo.entity.User;

/**
 * Utility class to access the currently authenticated user.
 */
@Component
public class SecurityUtils {

    /**
     * Get the currently authenticated user.
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetailsService.CustomUserDetails) {
            return ((CustomUserDetailsService.CustomUserDetails) authentication.getPrincipal()).getUser();
        }
        return null;
    }

    /**
     * Get the currently authenticated user's ID.
     */
    public Long getCurrentUserId() {
        User user = getCurrentUser();
        return user != null ? user.getId() : null;
    }

    /**
     * Check if the current user is an administrator.
     */
    public boolean isCurrentUserAdmin() {
        User user = getCurrentUser();
        return user != null && user.isAdmin();
    }

    /**
     * Check if the current user is a student.
     */
    public boolean isCurrentUserStudent() {
        User user = getCurrentUser();
        return user != null && user.isStudent();
    }

    /**
     * Get the username of the currently authenticated user.
     */
    public String getCurrentUsername() {
        User user = getCurrentUser();
        return user != null ? user.getUsername() : null;
    }
}
