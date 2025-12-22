package com.example.demo.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * QuizResult entity representing the outcome of a student's quiz attempt.
 * Used by the Agentic AI to evaluate performance and decide course validation.
 */
@Entity
@Table(name = "quiz_results")
public class QuizResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(nullable = false)
    private int totalQuestions;

    @Column(nullable = false)
    private int correctAnswers;

    @Column(nullable = false)
    private double scorePercentage;

    @Column(nullable = false)
    private boolean passed;

    @Column(nullable = false)
    private double passingThreshold = 70.0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime completedAt;

    @Column(nullable = false)
    private int timeTakenSeconds;

    @Column(columnDefinition = "TEXT")
    private String agentFeedback;

    @Enumerated(EnumType.STRING)
    private DifficultyLevel recommendedNextDifficulty;

    @ElementCollection
    @CollectionTable(name = "quiz_answers", joinColumns = @JoinColumn(name = "result_id"))
    private List<StudentAnswer> studentAnswers = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        completedAt = LocalDateTime.now();
        calculateScore();
    }

    // Constructors
    public QuizResult() {}

    public QuizResult(Quiz quiz, User student, int totalQuestions, int timeTakenSeconds) {
        this.quiz = quiz;
        this.student = student;
        this.totalQuestions = totalQuestions;
        this.timeTakenSeconds = timeTakenSeconds;
    }

    // Business methods
    public void calculateScore() {
        if (totalQuestions > 0) {
            this.scorePercentage = ((double) correctAnswers / totalQuestions) * 100;
            this.passed = this.scorePercentage >= this.passingThreshold;
        }
    }

    public void addAnswer(StudentAnswer answer) {
        studentAnswers.add(answer);
        if (answer.isCorrect()) {
            correctAnswers++;
        }
        calculateScore();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
    }

    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(int totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public int getCorrectAnswers() {
        return correctAnswers;
    }

    public void setCorrectAnswers(int correctAnswers) {
        this.correctAnswers = correctAnswers;
    }

    public double getScorePercentage() {
        return scorePercentage;
    }

    public void setScorePercentage(double scorePercentage) {
        this.scorePercentage = scorePercentage;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public double getPassingThreshold() {
        return passingThreshold;
    }

    public void setPassingThreshold(double passingThreshold) {
        this.passingThreshold = passingThreshold;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public int getTimeTakenSeconds() {
        return timeTakenSeconds;
    }

    public void setTimeTakenSeconds(int timeTakenSeconds) {
        this.timeTakenSeconds = timeTakenSeconds;
    }

    public String getAgentFeedback() {
        return agentFeedback;
    }

    public void setAgentFeedback(String agentFeedback) {
        this.agentFeedback = agentFeedback;
    }

    public DifficultyLevel getRecommendedNextDifficulty() {
        return recommendedNextDifficulty;
    }

    public void setRecommendedNextDifficulty(DifficultyLevel recommendedNextDifficulty) {
        this.recommendedNextDifficulty = recommendedNextDifficulty;
    }

    public List<StudentAnswer> getStudentAnswers() {
        return studentAnswers;
    }

    public void setStudentAnswers(List<StudentAnswer> studentAnswers) {
        this.studentAnswers = studentAnswers;
    }
}
