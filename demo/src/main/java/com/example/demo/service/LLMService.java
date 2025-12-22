package com.example.demo.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.demo.entity.DifficultyLevel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;

/**
 * LLM (Large Language Model) Service using Google Gemini API with official SDK.
 */
@Service
public class LLMService {

    private static final Logger logger = LoggerFactory.getLogger(LLMService.class);

    private final ObjectMapper objectMapper;
    private Client geminiClient;

    @Value("${app.gemini.api-key:}")
    private String geminiApiKey;

    // gemini-2.5-flash-lite has the best rate limits (10 RPM)
    private static final String GEMINI_MODEL = "gemini-2.5-flash-lite";
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_DELAY_MS = 5000; // 5 seconds

    public LLMService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Initialize the Gemini client lazily with API key.
     */
    private Client getGeminiClient() {
        if (geminiClient == null && geminiApiKey != null && !geminiApiKey.isBlank()) {
            geminiClient = Client.builder().apiKey(geminiApiKey).build();
            logger.info("Initialized Gemini client with model: {}", GEMINI_MODEL);
        }
        return geminiClient;
    }

    /**
     * Generate quiz questions using Gemini LLM via official SDK with retry logic.
     */
    public LLMModels.QuizResponse generateQuiz(String context, int numberOfQuestions, 
                                                DifficultyLevel difficulty, String courseTitle) {
        logger.info("Starting quiz generation - API key present: {}", (geminiApiKey != null && !geminiApiKey.isBlank()));
        
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            logger.warn("Gemini API key not configured - using mock mode");
            return generateMockQuiz(context, numberOfQuestions, difficulty, courseTitle);
        }

        String prompt = buildQuizPrompt(context, numberOfQuestions, difficulty, courseTitle);
        
        // Retry logic with exponential backoff
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                logger.info("Calling Gemini API ({}) - attempt {}/{}", GEMINI_MODEL, attempt, MAX_RETRIES);
                
                Client client = getGeminiClient();
                if (client == null) {
                    throw new RuntimeException("Failed to initialize Gemini client");
                }
                
                GenerateContentResponse response = client.models.generateContent(
                    GEMINI_MODEL,
                    prompt,
                    null
                );
                
                String responseText = response.text();
                logger.info("Received Gemini response ({} chars)", responseText.length());
                
                LLMModels.QuizResponse quizResponse = parseQuizResponse(responseText, numberOfQuestions, difficulty, context);
                quizResponse.setGeneratedByGemini(true);
                quizResponse.setModelUsed(GEMINI_MODEL);
                logger.info("Successfully generated quiz with {} questions", 
                           quizResponse.getQuestions() != null ? quizResponse.getQuestions().size() : 0);
                return quizResponse;
                
            } catch (Exception e) {
                String errorMsg = e.getMessage() != null ? e.getMessage() : "";
                logger.warn("Attempt {}/{} failed: {}", attempt, MAX_RETRIES, errorMsg);
                
                boolean isRateLimit = errorMsg.contains("429") || 
                                      errorMsg.toLowerCase().contains("rate") || 
                                      errorMsg.toLowerCase().contains("quota") ||
                                      errorMsg.toLowerCase().contains("resource_exhausted");
                
                if (isRateLimit && attempt < MAX_RETRIES) {
                    long delay = INITIAL_DELAY_MS * (long) Math.pow(2, attempt - 1);
                    logger.info("Rate limited - waiting {} seconds before retry...", delay / 1000);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else if (!isRateLimit) {
                    // Non-rate-limit error, don't retry
                    logger.error("Non-recoverable error calling Gemini API", e);
                    break;
                }
            }
        }
        
        logger.warn("All retries exhausted - falling back to mock mode");
        LLMModels.QuizResponse mockResponse = generateMockQuiz(context, numberOfQuestions, difficulty, courseTitle);
        mockResponse.setModelUsed("mock (rate-limited)");
        return mockResponse;
    }

    /**
     * Evaluate quiz results using Gemini.
     */
    public LLMModels.EvaluationResponse evaluateQuizResults(String courseContext, 
                                                            double scorePercentage,
                                                            int correctAnswers,
                                                            int totalQuestions,
                                                            List<String> incorrectTopics) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            return generateMockEvaluation(scorePercentage, correctAnswers, totalQuestions);
        }

        try {
            String prompt = buildEvaluationPrompt(scorePercentage, correctAnswers, totalQuestions, incorrectTopics);
            Client client = getGeminiClient();
            GenerateContentResponse response = client.models.generateContent(GEMINI_MODEL, prompt, null);
            return parseEvaluationResponse(response.text(), scorePercentage, correctAnswers, totalQuestions);
        } catch (Exception e) {
            logger.error("Error calling Gemini for evaluation: {}", e.getMessage());
            return generateMockEvaluation(scorePercentage, correctAnswers, totalQuestions);
        }
    }

    private LLMModels.QuizResponse parseQuizResponse(String responseText, int numberOfQuestions, 
                                                      DifficultyLevel difficulty, String context) {
        try {
            String jsonContent = extractJson(responseText);
            if (jsonContent != null) {
                JsonNode root = objectMapper.readTree(jsonContent);
                LLMModels.QuizResponse response = parseJsonToQuizResponse(root);
                if (response.getQuestions() != null && !response.getQuestions().isEmpty()) {
                    return response;
                }
            }
        } catch (Exception e) {
            logger.warn("Could not parse JSON response: {}", e.getMessage());
        }
        return generateMockQuiz(context, numberOfQuestions, difficulty, "Course");
    }

    private String extractJson(String text) {
        String cleaned = text.replaceAll("```json\\s*", "").replaceAll("```\\s*", "");
        int start = cleaned.indexOf("{");
        int end = cleaned.lastIndexOf("}");
        if (start >= 0 && end > start) {
            return cleaned.substring(start, end + 1);
        }
        return null;
    }

    private LLMModels.QuizResponse parseJsonToQuizResponse(JsonNode root) {
        LLMModels.QuizResponse response = new LLMModels.QuizResponse();
        List<LLMModels.QuestionData> questions = new ArrayList<>();

        JsonNode questionsNode = root.path("questions");
        if (questionsNode.isArray()) {
            for (JsonNode qNode : questionsNode) {
                LLMModels.QuestionData question = new LLMModels.QuestionData();
                question.setQuestionText(qNode.path("question_text").asText());
                question.setCorrectOptionIndex(qNode.path("correct_option_index").asInt(0));
                question.setExplanation(qNode.path("explanation").asText(""));
                question.setSourceContext(qNode.path("source_context").asText(""));

                List<LLMModels.OptionData> options = new ArrayList<>();
                JsonNode optionsNode = qNode.path("options");
                if (optionsNode.isArray()) {
                    for (JsonNode optNode : optionsNode) {
                        LLMModels.OptionData option = new LLMModels.OptionData();
                        option.setText(optNode.path("text").asText());
                        option.setExplanation(optNode.path("explanation").asText(""));
                        options.add(option);
                    }
                }
                question.setOptions(options);
                questions.add(question);
            }
        }
        response.setQuestions(questions);
        return response;
    }

    private String buildQuizPrompt(String context, int numberOfQuestions, 
                                   DifficultyLevel difficulty, String courseTitle) {
        return String.format("""
            You are an expert educational quiz creator. Generate a multiple-choice quiz 
            based EXCLUSIVELY on the following course content.
            
            COURSE TITLE: %s
            DIFFICULTY LEVEL: %s
            NUMBER OF QUESTIONS: %d
            
            COURSE CONTENT:
            %s
            
            REQUIREMENTS:
            1. Each question must have exactly 4 answer options
            2. Exactly ONE option must be correct
            3. Questions must be derived ONLY from the provided content
            
            Respond ONLY with valid JSON:
            {
              "questions": [
                {
                  "question_text": "Question text",
                  "options": [
                    {"text": "Option A", "explanation": "Why correct/incorrect"},
                    {"text": "Option B", "explanation": "Why correct/incorrect"},
                    {"text": "Option C", "explanation": "Why correct/incorrect"},
                    {"text": "Option D", "explanation": "Why correct/incorrect"}
                  ],
                  "correct_option_index": 0,
                  "explanation": "Overall explanation",
                  "source_context": "Source from content"
                }
              ]
            }
            """, courseTitle, difficulty.name(), numberOfQuestions, context);
    }

    private String buildEvaluationPrompt(double scorePercentage, int correctAnswers, 
                                         int totalQuestions, List<String> incorrectTopics) {
        return String.format("""
            Evaluate quiz results: Score: %.1f%%, Correct: %d/%d, Weak topics: %s
            
            Return JSON: {"feedback": "message", "strengths": [], "weaknesses": [], 
            "recommendations": [], "recommended_difficulty": "EASY|MEDIUM|HARD|EXPERT", 
            "course_validated": true/false}
            """, scorePercentage, correctAnswers, totalQuestions, 
            incorrectTopics != null ? String.join(", ", incorrectTopics) : "none");
    }

    private LLMModels.EvaluationResponse parseEvaluationResponse(String responseText, 
                                                                   double scorePercentage,
                                                                   int correctAnswers,
                                                                   int totalQuestions) {
        try {
            String jsonContent = extractJson(responseText);
            if (jsonContent != null) {
                JsonNode root = objectMapper.readTree(jsonContent);
                LLMModels.EvaluationResponse response = new LLMModels.EvaluationResponse();
                response.setFeedback(root.path("feedback").asText("Good effort!"));
                response.setCourseValidated(root.path("course_validated").asBoolean(scorePercentage >= 70));
                
                String diffStr = root.path("recommended_difficulty").asText("MEDIUM");
                try {
                    response.setRecommendedDifficulty(DifficultyLevel.valueOf(diffStr));
                } catch (Exception e) {
                    response.setRecommendedDifficulty(DifficultyLevel.MEDIUM);
                }

                List<String> strengths = new ArrayList<>();
                root.path("strengths").forEach(n -> strengths.add(n.asText()));
                response.setStrengths(strengths);

                List<String> weaknesses = new ArrayList<>();
                root.path("weaknesses").forEach(n -> weaknesses.add(n.asText()));
                response.setWeaknesses(weaknesses);

                List<String> recommendations = new ArrayList<>();
                root.path("recommendations").forEach(n -> recommendations.add(n.asText()));
                response.setRecommendations(recommendations);

                return response;
            }
        } catch (Exception e) {
            logger.warn("Could not parse evaluation: {}", e.getMessage());
        }
        return generateMockEvaluation(scorePercentage, correctAnswers, totalQuestions);
    }

    private LLMModels.QuizResponse generateMockQuiz(String context, int numberOfQuestions, 
                                                     DifficultyLevel difficulty, String courseTitle) {
        LLMModels.QuizResponse response = new LLMModels.QuizResponse();
        List<LLMModels.QuestionData> questions = new ArrayList<>();

        String[] paragraphs = context.split("\\n\\n+");
        
        for (int i = 0; i < numberOfQuestions; i++) {
            String paragraph = paragraphs[i % paragraphs.length];
            LLMModels.QuestionData question = createMockQuestion(paragraph, i, difficulty);
            questions.add(question);
        }

        response.setQuestions(questions);
        response.setModelUsed("mock");
        return response;
    }

    private LLMModels.QuestionData createMockQuestion(String paragraph, int index, DifficultyLevel difficulty) {
        LLMModels.QuestionData question = new LLMModels.QuestionData();
        String[] sentences = paragraph.split("\\. ");
        String mainSentence = sentences.length > 0 ? sentences[0] : paragraph;
        
        question.setQuestionText("Question " + (index + 1) + ": What is the key concept in this section?");
        question.setSourceContext(mainSentence.length() > 100 ? mainSentence.substring(0, 100) + "..." : mainSentence);
        question.setCorrectOptionIndex(0);
        question.setExplanation("This reflects the course content.");

        List<LLMModels.OptionData> options = new ArrayList<>();
        
        LLMModels.OptionData opt1 = new LLMModels.OptionData();
        opt1.setText("Correct: " + (mainSentence.length() > 50 ? mainSentence.substring(0, 50) + "..." : mainSentence));
        opt1.setExplanation("Correct answer from the course.");
        options.add(opt1);

        for (int i = 1; i <= 3; i++) {
            LLMModels.OptionData opt = new LLMModels.OptionData();
            opt.setText("Distractor option " + i);
            opt.setExplanation("Incorrect - not from course content.");
            options.add(opt);
        }

        question.setOptions(options);
        return question;
    }

    private LLMModels.EvaluationResponse generateMockEvaluation(double scorePercentage, 
                                                                 int correctAnswers, 
                                                                 int totalQuestions) {
        LLMModels.EvaluationResponse response = new LLMModels.EvaluationResponse();

        if (scorePercentage >= 70) {
            response.setFeedback("Good job! You passed the quiz.");
            response.setStrengths(List.of("Good understanding"));
            response.setWeaknesses(List.of());
            response.setRecommendedDifficulty(DifficultyLevel.HARD);
            response.setCourseValidated(true);
            response.setRecommendations(List.of("Try a harder quiz"));
        } else {
            response.setFeedback("Keep studying! Review the material.");
            response.setStrengths(List.of("Effort shown"));
            response.setWeaknesses(List.of("Needs more review"));
            response.setRecommendedDifficulty(DifficultyLevel.EASY);
            response.setCourseValidated(false);
            response.setRecommendations(List.of("Re-read course content"));
        }

        return response;
    }

    public boolean isLLMAvailable() {
        return geminiApiKey != null && !geminiApiKey.isBlank();
    }
}
