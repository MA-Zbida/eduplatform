package com.example.demo.service;

import java.util.List;

import com.example.demo.entity.DifficultyLevel;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO classes for LLM communication.
 * These structures define the expected format for quiz generation responses.
 */
public class LLMModels {

    /**
     * Complete quiz response from LLM.
     */
    public static class QuizResponse {
        @JsonProperty("questions")
        private List<QuestionData> questions;

        // Metadata about the generation
        private boolean generatedByGemini = false;
        private String modelUsed = "mock";

        public List<QuestionData> getQuestions() {
            return questions;
        }

        public void setQuestions(List<QuestionData> questions) {
            this.questions = questions;
        }

        public boolean isGeneratedByGemini() {
            return generatedByGemini;
        }

        public void setGeneratedByGemini(boolean generatedByGemini) {
            this.generatedByGemini = generatedByGemini;
        }

        public String getModelUsed() {
            return modelUsed;
        }

        public void setModelUsed(String modelUsed) {
            this.modelUsed = modelUsed;
        }
    }

    /**
     * Individual question data from LLM.
     */
    public static class QuestionData {
        @JsonProperty("question_text")
        private String questionText;

        @JsonProperty("options")
        private List<OptionData> options;

        @JsonProperty("correct_option_index")
        private int correctOptionIndex;

        @JsonProperty("explanation")
        private String explanation;

        @JsonProperty("source_context")
        private String sourceContext;

        public String getQuestionText() {
            return questionText;
        }

        public void setQuestionText(String questionText) {
            this.questionText = questionText;
        }

        public List<OptionData> getOptions() {
            return options;
        }

        public void setOptions(List<OptionData> options) {
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

        public String getSourceContext() {
            return sourceContext;
        }

        public void setSourceContext(String sourceContext) {
            this.sourceContext = sourceContext;
        }
    }

    /**
     * Answer option data from LLM.
     */
    public static class OptionData {
        @JsonProperty("text")
        private String text;

        @JsonProperty("explanation")
        private String explanation;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getExplanation() {
            return explanation;
        }

        public void setExplanation(String explanation) {
            this.explanation = explanation;
        }
    }

    /**
     * Evaluation response from LLM for quiz results.
     */
    public static class EvaluationResponse {
        @JsonProperty("feedback")
        private String feedback;

        @JsonProperty("strengths")
        private List<String> strengths;

        @JsonProperty("weaknesses")
        private List<String> weaknesses;

        @JsonProperty("recommended_difficulty")
        private DifficultyLevel recommendedDifficulty;

        @JsonProperty("course_validated")
        private boolean courseValidated;

        @JsonProperty("recommendations")
        private List<String> recommendations;

        public String getFeedback() {
            return feedback;
        }

        public void setFeedback(String feedback) {
            this.feedback = feedback;
        }

        public List<String> getStrengths() {
            return strengths;
        }

        public void setStrengths(List<String> strengths) {
            this.strengths = strengths;
        }

        public List<String> getWeaknesses() {
            return weaknesses;
        }

        public void setWeaknesses(List<String> weaknesses) {
            this.weaknesses = weaknesses;
        }

        public DifficultyLevel getRecommendedDifficulty() {
            return recommendedDifficulty;
        }

        public void setRecommendedDifficulty(DifficultyLevel recommendedDifficulty) {
            this.recommendedDifficulty = recommendedDifficulty;
        }

        public boolean isCourseValidated() {
            return courseValidated;
        }

        public void setCourseValidated(boolean courseValidated) {
            this.courseValidated = courseValidated;
        }

        public List<String> getRecommendations() {
            return recommendations;
        }

        public void setRecommendations(List<String> recommendations) {
            this.recommendations = recommendations;
        }
    }
}
