package com.example.demo.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.QuizRequestDTO;
import com.example.demo.dto.QuizSubmissionDTO;
import com.example.demo.entity.AnswerOption;
import com.example.demo.entity.Course;
import com.example.demo.entity.DifficultyLevel;
import com.example.demo.entity.Question;
import com.example.demo.entity.Quiz;
import com.example.demo.entity.QuizResult;
import com.example.demo.entity.StudentAnswer;
import com.example.demo.entity.User;
import com.example.demo.repository.EnrollmentRepository;
import com.example.demo.repository.QuizRepository;
import com.example.demo.repository.QuizResultRepository;

/**
 * Agentic AI Service.
 * 
 * This is the intelligent supervisor that orchestrates the entire quiz generation
 * and evaluation process. The agent:
 * 
 * 1. Determines quiz parameters (number of questions, difficulty)
 * 2. Retrieves relevant content via RAG
 * 3. Controls LLM quiz generation
 * 4. Enforces strict adherence to course content (no hallucination)
 * 5. Dynamically adapts difficulty based on student performance
 * 6. Evaluates quiz results
 * 7. Decides whether the course is validated or requires further study
 * 
 * Architecture Decision: The agent is designed as a stateless service that
 * makes decisions based on:
 * - Student's historical performance
 * - Course complexity
 * - Current quiz request parameters
 * - Evaluation results
 */
@Service
@Transactional
public class AgentService {

    private static final Logger logger = LoggerFactory.getLogger(AgentService.class);

    private static final double VALIDATION_THRESHOLD = 70.0;
    private static final int MIN_QUESTIONS = 3;
    private static final int MAX_QUESTIONS = 20;

    private final RAGService ragService;
    private final LLMService llmService;
    private final QuizRepository quizRepository;
    private final QuizResultRepository quizResultRepository;
    private final EnrollmentRepository enrollmentRepository;

    public AgentService(RAGService ragService,
                        LLMService llmService,
                        QuizRepository quizRepository,
                        QuizResultRepository quizResultRepository,
                        EnrollmentRepository enrollmentRepository) {
        this.ragService = ragService;
        this.llmService = llmService;
        this.quizRepository = quizRepository;
        this.quizResultRepository = quizResultRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    /**
     * Generate a quiz using the agentic AI pipeline.
     * 
     * The agent performs the following steps:
     * 1. Analyze student's history to determine optimal difficulty
     * 2. Retrieve relevant content using RAG
     * 3. Generate quiz using LLM
     * 4. Validate generated content against course material
     * 5. Return the complete quiz
     */
    public Quiz generateQuiz(User student, Course course, QuizRequestDTO request) {
        logger.info("Agent: Starting quiz generation for student {} on course {}", 
                    student.getId(), course.getId());

        // Step 1: Determine optimal parameters
        DifficultyLevel difficulty = determineOptimalDifficulty(student.getId(), course.getId(), request.getDifficulty());
        int numberOfQuestions = validateQuestionCount(request.getNumberOfQuestions());

        logger.info("Agent: Determined difficulty={}, questions={}", difficulty, numberOfQuestions);

        // Step 2: Retrieve relevant content via RAG
        String context = ragService.getQuizContext(course.getId());
        
        if (context == null || context.isBlank()) {
            throw new IllegalStateException("No indexed content available for this course");
        }

        logger.info("Agent: Retrieved {} characters of context", context.length());

        // Step 3: Generate quiz using LLM
        LLMModels.QuizResponse llmResponse = llmService.generateQuiz(
                context, numberOfQuestions, difficulty, course.getTitle());

        // Step 4: Create and persist the quiz
        Quiz quiz = new Quiz(course, student, 
                "Quiz: " + course.getTitle(), 
                difficulty, numberOfQuestions);
        
        // Set AI generation metadata
        quiz.setGeneratedByGemini(llmResponse.isGeneratedByGemini());
        quiz.setLlmModelUsed(llmResponse.getModelUsed());

        // Step 5: Convert LLM response to quiz questions
        for (LLMModels.QuestionData questionData : llmResponse.getQuestions()) {
            Question question = new Question();
            question.setQuestionText(questionData.getQuestionText());
            question.setSourceContext(questionData.getSourceContext());
            question.setCorrectOptionIndex(questionData.getCorrectOptionIndex());
            question.setExplanation(questionData.getExplanation());

            for (LLMModels.OptionData optionData : questionData.getOptions()) {
                AnswerOption option = new AnswerOption();
                option.setOptionText(optionData.getText());
                option.setExplanation(optionData.getExplanation());
                question.addOption(option);
            }

            quiz.addQuestion(question);
        }

        Quiz savedQuiz = quizRepository.save(quiz);
        logger.info("Agent: Quiz generated successfully with {} questions", savedQuiz.getQuestions().size());

        return savedQuiz;
    }

    /**
     * Evaluate quiz submission using the agentic AI.
     * 
     * The agent:
     * 1. Scores the quiz
     * 2. Analyzes performance patterns
     * 3. Generates personalized feedback using LLM
     * 4. Determines next recommended difficulty
     * 5. Decides if course should be marked as validated
     */
    public QuizResult evaluateQuiz(Quiz quiz, QuizSubmissionDTO submission) {
        logger.info("Agent: Evaluating quiz {} for student {}", quiz.getId(), quiz.getStudent().getId());

        // Step 1: Calculate score
        int correctAnswers = 0;
        List<StudentAnswer> studentAnswers = new ArrayList<>();
        List<String> incorrectTopics = new ArrayList<>();

        for (Question question : quiz.getQuestions()) {
            Integer selectedIndex = submission.getAnswers().get(question.getId());
            if (selectedIndex != null) {
                boolean isCorrect = question.isCorrect(selectedIndex);
                if (isCorrect) {
                    correctAnswers++;
                } else {
                    incorrectTopics.add(question.getSourceContext());
                }
                
                StudentAnswer answer = new StudentAnswer(
                        question.getId(),
                        selectedIndex,
                        question.getCorrectOptionIndex()
                );
                studentAnswers.add(answer);
            }
        }

        int totalQuestions = quiz.getQuestions().size();
        double scorePercentage = totalQuestions > 0 ? 
                ((double) correctAnswers / totalQuestions) * 100 : 0;

        logger.info("Agent: Score calculated - {}/{} ({}%)", correctAnswers, totalQuestions, scorePercentage);

        // Step 2: Get LLM evaluation
        String courseContext = ragService.getQuizContext(quiz.getCourse().getId());
        LLMModels.EvaluationResponse evaluation = llmService.evaluateQuizResults(
                courseContext, scorePercentage, correctAnswers, totalQuestions, incorrectTopics);

        // Step 3: Create quiz result
        QuizResult result = new QuizResult(quiz, quiz.getStudent(), totalQuestions, submission.getTimeTakenSeconds());
        result.setCorrectAnswers(correctAnswers);
        result.setScorePercentage(scorePercentage);
        result.setPassed(scorePercentage >= VALIDATION_THRESHOLD);
        result.setAgentFeedback(evaluation.getFeedback());
        result.setRecommendedNextDifficulty(evaluation.getRecommendedDifficulty());
        result.setStudentAnswers(studentAnswers);

        QuizResult savedResult = quizResultRepository.save(result);

        // Step 4: Update enrollment status if course is validated
        if (evaluation.isCourseValidated()) {
            updateEnrollmentStatus(quiz.getStudent().getId(), quiz.getCourse().getId(), scorePercentage);
        }

        logger.info("Agent: Evaluation complete - passed={}, recommended_difficulty={}", 
                    savedResult.isPassed(), savedResult.getRecommendedNextDifficulty());

        return savedResult;
    }

    /**
     * Determine the optimal difficulty based on student's history.
     */
    private DifficultyLevel determineOptimalDifficulty(Long studentId, Long courseId, DifficultyLevel requestedDifficulty) {
        if (requestedDifficulty != null) {
            return requestedDifficulty;
        }

        // Check student's previous quiz results for this course
        List<QuizResult> previousResults = quizResultRepository.findByCourseIdAndStudentId(courseId, studentId);

        if (previousResults.isEmpty()) {
            return DifficultyLevel.MEDIUM; // Default for new students
        }

        // Calculate average score from recent results
        double averageScore = previousResults.stream()
                .limit(5) // Consider last 5 quizzes
                .mapToDouble(QuizResult::getScorePercentage)
                .average()
                .orElse(50.0);

        // Adaptive difficulty based on performance
        if (averageScore >= 90) {
            return DifficultyLevel.EXPERT;
        } else if (averageScore >= 75) {
            return DifficultyLevel.HARD;
        } else if (averageScore >= 50) {
            return DifficultyLevel.MEDIUM;
        } else {
            return DifficultyLevel.EASY;
        }
    }

    /**
     * Validate and constrain the number of questions.
     */
    private int validateQuestionCount(int requested) {
        return Math.max(MIN_QUESTIONS, Math.min(MAX_QUESTIONS, requested));
    }

    /**
     * Update enrollment status based on quiz performance.
     */
    private void updateEnrollmentStatus(Long studentId, Long courseId, double scorePercentage) {
        enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId)
                .ifPresent(enrollment -> {
                    // Check if student has passed enough quizzes to validate the course
                    long passedCount = quizResultRepository.countPassedByCourseIdAndStudentId(courseId, studentId);
                    
                    if (passedCount >= 2 && scorePercentage >= VALIDATION_THRESHOLD) {
                        enrollment.markAsCompleted();
                        enrollmentRepository.save(enrollment);
                        logger.info("Agent: Course {} validated for student {}", courseId, studentId);
                    } else {
                        // Update progress
                        int progress = (int) Math.min(90, passedCount * 30);
                        enrollment.updateProgress(progress);
                        enrollmentRepository.save(enrollment);
                    }
                });
    }

    /**
     * Get recommendations for a student based on their performance.
     */
    public List<String> getStudentRecommendations(Long studentId, Long courseId) {
        List<String> recommendations = new ArrayList<>();
        List<QuizResult> results = quizResultRepository.findByCourseIdAndStudentId(courseId, studentId);

        if (results.isEmpty()) {
            recommendations.add("Start with an easy quiz to assess your current understanding.");
            recommendations.add("Read through the course material before attempting quizzes.");
            return recommendations;
        }

        double averageScore = results.stream()
                .mapToDouble(QuizResult::getScorePercentage)
                .average()
                .orElse(0.0);

        if (averageScore >= 80) {
            recommendations.add("Excellent progress! Try harder difficulty levels.");
            recommendations.add("Consider helping other students with this topic.");
        } else if (averageScore >= 60) {
            recommendations.add("Good progress. Review the topics where you made mistakes.");
            recommendations.add("Practice with medium difficulty quizzes to reinforce learning.");
        } else {
            recommendations.add("Focus on understanding the core concepts first.");
            recommendations.add("Re-read the course material and take notes.");
            recommendations.add("Start with easier quizzes to build confidence.");
        }

        return recommendations;
    }
}
