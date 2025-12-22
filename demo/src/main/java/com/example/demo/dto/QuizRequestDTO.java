package com.example.demo.dto;

import com.example.demo.entity.DifficultyLevel;

/**
 * DTO for quiz generation request.
 */
public class QuizRequestDTO {

    private Long courseId;
    private int numberOfQuestions = 5;
    private DifficultyLevel difficulty;

    // Constructors
    public QuizRequestDTO() {}

    public QuizRequestDTO(Long courseId, int numberOfQuestions, DifficultyLevel difficulty) {
        this.courseId = courseId;
        this.numberOfQuestions = numberOfQuestions;
        this.difficulty = difficulty;
    }

    // Getters and Setters
    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public int getNumberOfQuestions() {
        return numberOfQuestions;
    }

    public void setNumberOfQuestions(int numberOfQuestions) {
        this.numberOfQuestions = numberOfQuestions;
    }

    public DifficultyLevel getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(DifficultyLevel difficulty) {
        this.difficulty = difficulty;
    }
}
