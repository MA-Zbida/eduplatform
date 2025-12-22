package com.example.demo.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.demo.security.CustomUserDetailsService;

/**
 * Controller for authentication and public pages.
 */
@Controller
public class AuthController {

    @GetMapping("/")
    public String home(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            var userDetails = (CustomUserDetailsService.CustomUserDetails) authentication.getPrincipal();
            if (userDetails.isAdmin()) {
                return "redirect:/admin/dashboard";
            } else {
                return "redirect:/student/dashboard";
            }
        }
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/";
        }
        return "login";
    }

    @GetMapping("/access-denied")
    public String accessDenied(Model model) {
        model.addAttribute("message", "You do not have permission to access this resource.");
        return "error/access-denied";
    }
}
