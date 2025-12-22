package com.example.demo.dto;

/**
 * DTO for dashboard statistics.
 */
public class DashboardStatsDTO {

    private long totalCourses;
    private long publishedCourses;
    private long totalStudents;
    private long totalQuizzes;
    private long totalEnrollments;
    private Double averageQuizScore;

    // Constructors
    public DashboardStatsDTO() {}

    public DashboardStatsDTO(long totalCourses, long publishedCourses, long totalStudents, 
                             long totalQuizzes, long totalEnrollments, Double averageQuizScore) {
        this.totalCourses = totalCourses;
        this.publishedCourses = publishedCourses;
        this.totalStudents = totalStudents;
        this.totalQuizzes = totalQuizzes;
        this.totalEnrollments = totalEnrollments;
        this.averageQuizScore = averageQuizScore;
    }

    // Getters and Setters
    public long getTotalCourses() {
        return totalCourses;
    }

    public void setTotalCourses(long totalCourses) {
        this.totalCourses = totalCourses;
    }

    public long getPublishedCourses() {
        return publishedCourses;
    }

    public void setPublishedCourses(long publishedCourses) {
        this.publishedCourses = publishedCourses;
    }

    public long getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(long totalStudents) {
        this.totalStudents = totalStudents;
    }

    public long getTotalQuizzes() {
        return totalQuizzes;
    }

    public void setTotalQuizzes(long totalQuizzes) {
        this.totalQuizzes = totalQuizzes;
    }

    public long getTotalEnrollments() {
        return totalEnrollments;
    }

    public void setTotalEnrollments(long totalEnrollments) {
        this.totalEnrollments = totalEnrollments;
    }

    public Double getAverageQuizScore() {
        return averageQuizScore;
    }

    public void setAverageQuizScore(Double averageQuizScore) {
        this.averageQuizScore = averageQuizScore;
    }
}
