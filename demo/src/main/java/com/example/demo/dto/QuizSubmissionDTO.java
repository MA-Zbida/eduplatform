package com.example.demo.dto;

import java.util.Map;

/**
 * DTO for quiz submission containing student answers.
 */
public class QuizSubmissionDTO {

    private Long quizId;
    private Map<Long, Integer> answers; // questionId -> selectedOptionIndex
    private int timeTakenSeconds;

    // Constructors
    public QuizSubmissionDTO() {}

    public QuizSubmissionDTO(Long quizId, Map<Long, Integer> answers, int timeTakenSeconds) {
        this.quizId = quizId;
        this.answers = answers;
        this.timeTakenSeconds = timeTakenSeconds;
    }

    // Getters and Setters
    public Long getQuizId() {
        return quizId;
    }

    public void setQuizId(Long quizId) {
        this.quizId = quizId;
    }

    public Map<Long, Integer> getAnswers() {
        return answers;
    }

    public void setAnswers(Map<Long, Integer> answers) {
        this.answers = answers;
    }

    public int getTimeTakenSeconds() {
        return timeTakenSeconds;
    }

    public void setTimeTakenSeconds(int timeTakenSeconds) {
        this.timeTakenSeconds = timeTakenSeconds;
    }
}
