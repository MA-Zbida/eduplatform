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
                
                // Only set model if successfully parsed from Gemini (flag is set in parseQuizResponse)
                if (quizResponse.isGeneratedByGemini()) {
                    quizResponse.setModelUsed(GEMINI_MODEL);
                    logger.info("Successfully generated quiz with {} questions from Gemini", 
                               quizResponse.getQuestions() != null ? quizResponse.getQuestions().size() : 0);
                } else {
                    logger.warn("Gemini returned response but parsing failed, using mock quiz");
                }
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
                                                            List<String> incorrectTopics,
                                                            DifficultyLevel currentDifficulty) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            return generateMockEvaluation(scorePercentage, correctAnswers, totalQuestions, currentDifficulty);
        }

        try {
            String prompt = buildEvaluationPrompt(scorePercentage, correctAnswers, totalQuestions, incorrectTopics, currentDifficulty);
            Client client = getGeminiClient();
            GenerateContentResponse response = client.models.generateContent(GEMINI_MODEL, prompt, null);
            return parseEvaluationResponse(response.text(), scorePercentage, correctAnswers, totalQuestions, currentDifficulty);
        } catch (Exception e) {
            logger.error("Error calling Gemini for evaluation: {}", e.getMessage());
            return generateMockEvaluation(scorePercentage, correctAnswers, totalQuestions, currentDifficulty);
        }
    }

    private LLMModels.QuizResponse parseQuizResponse(String responseText, int numberOfQuestions, 
                                                      DifficultyLevel difficulty, String context) {
        try {
            String jsonContent = extractJson(responseText);
            logger.debug("Extracted JSON: {}", jsonContent != null ? jsonContent.substring(0, Math.min(200, jsonContent.length())) : "null");
            
            if (jsonContent != null) {
                JsonNode root = objectMapper.readTree(jsonContent);
                LLMModels.QuizResponse response = parseJsonToQuizResponse(root);
                if (response.getQuestions() != null && !response.getQuestions().isEmpty()) {
                    // Mark as successfully parsed from Gemini
                    response.setGeneratedByGemini(true);
                    logger.info("Successfully parsed {} questions from Gemini response", response.getQuestions().size());
                    return response;
                } else {
                    logger.warn("Parsed response has no questions, falling back to mock");
                }
            } else {
                logger.warn("Could not extract JSON from response: {}", 
                           responseText.substring(0, Math.min(500, responseText.length())));
            }
        } catch (Exception e) {
            logger.warn("Could not parse JSON response: {} - Response snippet: {}", 
                       e.getMessage(), 
                       responseText.substring(0, Math.min(500, responseText.length())));
        }
        
        // Return mock quiz (not generated by Gemini)
        LLMModels.QuizResponse mockResponse = generateMockQuiz(context, numberOfQuestions, difficulty, "Course");
        mockResponse.setGeneratedByGemini(false);
        mockResponse.setModelUsed("mock (parse-failed)");
        return mockResponse;
    }

    private String extractJson(String text) {
        String cleaned = text.replaceAll("```json\\s*", "").replaceAll("```\\s*", "");
        int start = cleaned.indexOf("{");
        int end = cleaned.lastIndexOf("}");
        if (start >= 0 && end > start) {
            String json = cleaned.substring(start, end + 1);
            // Fix invalid JSON escape sequences from LaTeX math notation
            json = fixLatexEscapes(json);
            return json;
        }
        return null;
    }
    
    /**
     * Fix LaTeX escape sequences that are invalid in JSON.
     * LaTeX uses backslash-pi, backslash-alpha, etc. which need to be double-escaped for JSON.
     */
    private String fixLatexEscapes(String json) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                if (isValidJsonEscape(next)) {
                    // Valid escape sequence, keep both the backslash and the escape character
                    result.append(c);
                    result.append(next);
                    i += 2; // Skip both characters
                } else {
                    // Invalid escape (likely LaTeX like \m, \a, \pi, etc.)
                    // Double-escape the backslash and keep the next character
                    result.append("\\\\");
                    result.append(next);
                    i += 2; // Skip both characters
                }
            } else {
                result.append(c);
                i++;
            }
        }
        return result.toString();
    }
    
    /**
     * Check if a character following a backslash is a valid JSON escape character.
     */
    private boolean isValidJsonEscape(char c) {
        // Valid JSON escapes: quote, backslash, slash, b, f, n, r, t, and u for unicode
        return c == '"' || c == '\\' || c == '/' || 
               c == 'b' || c == 'f' || c == 'n' || c == 'r' || c == 't' ||
               c == 'u';
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
            You are an expert educational quiz creator specializing in creating engaging, 
            dynamic questions. Generate a multiple-choice quiz based EXCLUSIVELY on the 
            following course content.
            
            COURSE TITLE: %s
            DIFFICULTY LEVEL: %s
            NUMBER OF QUESTIONS: %d
            
            COURSE CONTENT:
            %s
            
            REQUIREMENTS:
            1. Each question must have exactly 4 answer options
            2. Exactly ONE option must be correct
            3. Questions must be derived ONLY from the provided content
            4. Be dynamic and creative with question styles (conceptual, practical, analytical)
            
            MATHEMATICAL CONTENT GUIDELINES:
            - When the content includes formulas, equations, or mathematical expressions, 
              include them in your questions and options
            - Use LaTeX notation for ALL mathematical expressions:
              * Inline math: wrap with single dollar signs, e.g., $E = mc^2$
              * Display math: wrap with double dollar signs, e.g., $$\\frac{a}{b}$$
            - Common LaTeX examples:
              * Fractions: $\\frac{numerator}{denominator}$
              * Exponents: $x^2$, $e^{-x}$
              * Subscripts: $x_1$, $a_{n+1}$
              * Square roots: $\\sqrt{x}$, $\\sqrt[n]{x}$
              * Greek letters: $\\alpha$, $\\beta$, $\\gamma$, $\\pi$, $\\theta$
              * Summation: $\\sum_{i=1}^{n} x_i$
              * Integrals: $\\int_{a}^{b} f(x) dx$
              * Limits: $\\lim_{x \\to \\infty} f(x)$
              * Vectors: $\\vec{v}$, $\\mathbf{F}$
              * Matrices: $\\begin{pmatrix} a & b \\\\ c & d \\end{pmatrix}$
            - For non-mathematical content, create clear conceptual questions
            
            QUESTION VARIETY:
            - Include formula-based questions if the content has mathematical concepts
            - Ask about the meaning and application of formulas, not just memorization
            - Include "which formula applies" type questions when appropriate
            - Ask students to identify correct transformations or simplifications
            
            Respond ONLY with valid JSON (escape special characters properly):
            {
              "questions": [
                {
                  "question_text": "Question text with $inline math$ or $$display math$$",
                  "options": [
                    {"text": "Option A with $formula$ if needed", "explanation": "Why correct/incorrect"},
                    {"text": "Option B", "explanation": "Why correct/incorrect"},
                    {"text": "Option C", "explanation": "Why correct/incorrect"},
                    {"text": "Option D", "explanation": "Why correct/incorrect"}
                  ],
                  "correct_option_index": 0,
                  "explanation": "Overall explanation with $formulas$ if relevant",
                  "source_context": "Source from content"
                }
              ]
            }
            """, courseTitle, difficulty.name(), numberOfQuestions, context);
    }

    private String buildEvaluationPrompt(double scorePercentage, int correctAnswers, 
                                         int totalQuestions, List<String> incorrectTopics,
                                         DifficultyLevel currentDifficulty) {
        return String.format("""
            Evaluate quiz results and recommend the NEXT appropriate difficulty level.
            
            CURRENT QUIZ INFO:
            - Current Difficulty: %s
            - Score: %.1f%%
            - Correct: %d/%d
            - Weak topics: %s
            
            DIFFICULTY PROGRESSION RULES:
            - If score >= 90%% at current level, recommend NEXT HIGHER level
            - If score >= 70%% at current level, recommend SAME level or SLIGHTLY higher
            - If score < 70%%, recommend SAME level or LOWER level
            - Available levels in order: EASY -> MEDIUM -> HARD -> EXPERT
            - If already at EXPERT with score >= 90%%, recommend EXPERT
            - If already at EASY with score < 50%%, recommend EASY
            
            Return JSON: {"feedback": "message", "strengths": [], "weaknesses": [], 
            "recommendations": [], "recommended_difficulty": "EASY|MEDIUM|HARD|EXPERT", 
            "course_validated": true/false}
            """, currentDifficulty.name(), scorePercentage, correctAnswers, totalQuestions, 
            incorrectTopics != null ? String.join(", ", incorrectTopics) : "none");
    }

    private LLMModels.EvaluationResponse parseEvaluationResponse(String responseText, 
                                                                   double scorePercentage,
                                                                   int correctAnswers,
                                                                   int totalQuestions,
                                                                   DifficultyLevel currentDifficulty) {
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
        return generateMockEvaluation(scorePercentage, correctAnswers, totalQuestions, currentDifficulty);
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
                                                                 int totalQuestions,
                                                                 DifficultyLevel currentDifficulty) {
        LLMModels.EvaluationResponse response = new LLMModels.EvaluationResponse();
        
        // Intelligent difficulty recommendation based on current level and score
        DifficultyLevel nextDifficulty = calculateNextDifficulty(currentDifficulty, scorePercentage);

        if (scorePercentage >= 90) {
            response.setFeedback("Excellent performance! You've mastered this level.");
            response.setStrengths(List.of("Excellent understanding", "Strong grasp of concepts"));
            response.setWeaknesses(List.of());
            response.setCourseValidated(true);
            response.setRecommendations(List.of("Ready for the next challenge!"));
        } else if (scorePercentage >= 70) {
            response.setFeedback("Good job! You passed the quiz.");
            response.setStrengths(List.of("Good understanding"));
            response.setWeaknesses(List.of("Minor areas to review"));
            response.setCourseValidated(true);
            response.setRecommendations(List.of("Review incorrect answers before moving on"));
        } else if (scorePercentage >= 50) {
            response.setFeedback("You're making progress. Keep practicing!");
            response.setStrengths(List.of("Effort shown", "Partial understanding"));
            response.setWeaknesses(List.of("Some concepts need more review"));
            response.setCourseValidated(false);
            response.setRecommendations(List.of("Focus on the topics you missed"));
        } else {
            response.setFeedback("Keep studying! Review the material carefully.");
            response.setStrengths(List.of("Taking initiative to learn"));
            response.setWeaknesses(List.of("Core concepts need reinforcement"));
            response.setCourseValidated(false);
            response.setRecommendations(List.of("Re-read course content", "Try easier questions first"));
        }
        
        response.setRecommendedDifficulty(nextDifficulty);
        return response;
    }
    
    /**
     * Calculate the next recommended difficulty based on current level and performance.
     * 
     * Logic:
     * - Score >= 90%: Move UP one level (or stay at EXPERT)
     * - Score >= 70%: Stay at current level or move up slightly
     * - Score >= 50%: Stay at current level
     * - Score < 50%: Move DOWN one level (or stay at EASY)
     */
    private DifficultyLevel calculateNextDifficulty(DifficultyLevel current, double scorePercentage) {
        DifficultyLevel[] levels = DifficultyLevel.values();
        int currentIndex = current.ordinal();
        
        if (scorePercentage >= 90) {
            // Excellent: Move up one level
            return levels[Math.min(currentIndex + 1, levels.length - 1)];
        } else if (scorePercentage >= 70) {
            // Good: Stay at current level (consolidate knowledge)
            return current;
        } else if (scorePercentage >= 50) {
            // Moderate: Stay at current level
            return current;
        } else {
            // Poor: Move down one level
            return levels[Math.max(currentIndex - 1, 0)];
        }
    }

    public boolean isLLMAvailable() {
        return geminiApiKey != null && !geminiApiKey.isBlank();
    }
}
