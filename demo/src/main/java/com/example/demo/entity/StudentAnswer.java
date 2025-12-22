package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Embeddable class representing a student's answer to a quiz question.
 */
@Embeddable
public class StudentAnswer {

    @Column(nullable = false)
    private Long questionId;

    @Column(nullable = false)
    private int selectedOptionIndex;

    @Column(nullable = false)
    private int correctOptionIndex;

    @Column(nullable = false)
    private boolean correct;

    // Constructors
    public StudentAnswer() {}

    public StudentAnswer(Long questionId, int selectedOptionIndex, int correctOptionIndex) {
        this.questionId = questionId;
        this.selectedOptionIndex = selectedOptionIndex;
        this.correctOptionIndex = correctOptionIndex;
        this.correct = selectedOptionIndex == correctOptionIndex;
    }

    // Getters and Setters
    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public int getSelectedOptionIndex() {
        return selectedOptionIndex;
    }

    public void setSelectedOptionIndex(int selectedOptionIndex) {
        this.selectedOptionIndex = selectedOptionIndex;
    }

    public int getCorrectOptionIndex() {
        return correctOptionIndex;
    }

    public void setCorrectOptionIndex(int correctOptionIndex) {
        this.correctOptionIndex = correctOptionIndex;
    }

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }
}
