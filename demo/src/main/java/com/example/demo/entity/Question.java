package com.example.demo.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Question entity representing a single question in a quiz.
 * Each question has exactly one correct answer with plausible distractors
 * and explanations for each answer option.
 */
@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(nullable = false)
    private int questionIndex;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String questionText;

    @Column(columnDefinition = "TEXT")
    private String sourceContext;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("optionIndex ASC")
    private List<AnswerOption> options = new ArrayList<>();

    @Column(nullable = false)
    private int correctOptionIndex;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    // Constructors
    public Question() {}

    public Question(String questionText, String sourceContext, int correctOptionIndex, String explanation) {
        this.questionText = questionText;
        this.sourceContext = sourceContext;
        this.correctOptionIndex = correctOptionIndex;
        this.explanation = explanation;
    }

    // Business methods
    public void addOption(AnswerOption option) {
        options.add(option);
        option.setQuestion(this);
        option.setOptionIndex(options.size() - 1);
    }

    public boolean isCorrect(int selectedOptionIndex) {
        return selectedOptionIndex == correctOptionIndex;
    }

    public AnswerOption getCorrectOption() {
        return options.stream()
                .filter(o -> o.getOptionIndex() == correctOptionIndex)
                .findFirst()
                .orElse(null);
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

    public int getQuestionIndex() {
        return questionIndex;
    }

    public void setQuestionIndex(int questionIndex) {
        this.questionIndex = questionIndex;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getSourceContext() {
        return sourceContext;
    }

    public void setSourceContext(String sourceContext) {
        this.sourceContext = sourceContext;
    }

    public List<AnswerOption> getOptions() {
        return options;
    }

    public void setOptions(List<AnswerOption> options) {
        this.options = options;
    }

    public int getCorrectOptionIndex() {
        return correctOptionIndex;
    }

    public void setCorrectOptionIndex(int correctOptionIndex) {
        this.correctOptionIndex = correctOptionIndex;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}
