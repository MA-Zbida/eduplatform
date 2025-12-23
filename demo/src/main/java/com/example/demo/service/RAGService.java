package com.example.demo.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Course;
import com.example.demo.entity.CourseChunk;
import com.example.demo.repository.CourseChunkRepository;

/**
 * RAG (Retrieval-Augmented Generation) Service.
 * 
 * This service handles:
 * 1. Chunking course content into manageable segments
 * 2. Indexing chunks for efficient retrieval
 * 3. Retrieving relevant content for quiz generation
 * 
 * Architecture Note: This v1 implementation uses text-based chunking and
 * simple keyword matching. The design is extensible to support:
 * - Vector embeddings with external vector databases
 * - Multi-modal content (PDF, images, video transcripts)
 * - Semantic similarity search
 */
@Service
@Transactional
public class RAGService {

    private static final Logger logger = LoggerFactory.getLogger(RAGService.class);

    private static final int DEFAULT_CHUNK_SIZE = 500;
    private static final int CHUNK_OVERLAP = 50;

    private final CourseChunkRepository chunkRepository;
    private final FileStorageService fileStorageService;

    public RAGService(CourseChunkRepository chunkRepository, FileStorageService fileStorageService) {
        this.chunkRepository = chunkRepository;
        this.fileStorageService = fileStorageService;
    }

    /**
     * Index a course by chunking its content.
     * This prepares the content for RAG-based retrieval.
     * Supports both text content and PDF documents.
     */
    public void indexCourse(Course course) {
        logger.info("Starting RAG indexing for course: {}", course.getId());

        // Clear existing chunks
        chunkRepository.deleteByCourseId(course.getId());

        // Get the content to index - extract from PDF if needed
        String contentToIndex = getIndexableContent(course);
        
        if (contentToIndex == null || contentToIndex.isBlank()) {
            logger.warn("No content available to index for course: {}", course.getId());
            return;
        }

        // Chunk the content
        List<CourseChunk> chunks = chunkContent(course, contentToIndex);

        // Save chunks
        chunkRepository.saveAll(chunks);

        logger.info("Indexed {} chunks for course: {}", chunks.size(), course.getId());
    }

    /**
     * Get the content to index for a course.
     * For PDF courses, extracts text from the PDF file.
     * For text courses, returns the text content directly.
     *
     * @param course the course to get content from
     * @return the text content to index
     */
    private String getIndexableContent(Course course) {
        // Check if course has a PDF
        if (course.hasPdf() && course.getPdfFilename() != null) {
            logger.info("Extracting text from PDF for course: {}", course.getId());
            String pdfText = fileStorageService.extractTextFromPdf(course.getPdfFilename());
            
            if (pdfText != null && !pdfText.isBlank()) {
                logger.info("Successfully extracted {} characters from PDF", pdfText.length());
                return pdfText;
            } else {
                logger.warn("PDF text extraction returned empty content for course: {}", course.getId());
                // Fall back to regular content if PDF extraction fails
                return course.getContent();
            }
        }
        
        // Return regular text content
        return course.getContent();
    }

    /**
     * Chunk course content into smaller, overlapping segments.
     * Uses a hybrid approach that works for both regular text and PDF-extracted content.
     */
    private List<CourseChunk> chunkContent(Course course, String content) {
        List<CourseChunk> chunks = new ArrayList<>();

        if (content == null || content.isBlank()) {
            return chunks;
        }

        // Normalize the content - replace multiple whitespace with single space/newline
        String normalizedContent = normalizeContent(content);
        
        // Try to split by natural boundaries (paragraphs, sections)
        List<String> segments = splitIntoSegments(normalizedContent);
        
        StringBuilder currentChunk = new StringBuilder();
        int chunkIndex = 0;
        int startPosition = 0;
        int currentPosition = 0;

        for (String segment : segments) {
            // If adding this segment exceeds chunk size, save current chunk
            if (currentChunk.length() + segment.length() > DEFAULT_CHUNK_SIZE && currentChunk.length() > 0) {
                CourseChunk chunk = new CourseChunk(
                        course,
                        currentChunk.toString().trim(),
                        chunkIndex++,
                        startPosition,
                        currentPosition
                );
                chunks.add(chunk);

                // Start new chunk with overlap
                int overlapStart = Math.max(0, currentChunk.length() - CHUNK_OVERLAP);
                currentChunk = new StringBuilder(currentChunk.substring(overlapStart));
                startPosition = currentPosition - (currentChunk.length());
            }

            // If a single segment is too large, split it by sentences or fixed size
            if (segment.length() > DEFAULT_CHUNK_SIZE) {
                List<String> subSegments = splitLargeSegment(segment);
                for (String subSegment : subSegments) {
                    if (currentChunk.length() + subSegment.length() > DEFAULT_CHUNK_SIZE && currentChunk.length() > 0) {
                        CourseChunk chunk = new CourseChunk(
                                course,
                                currentChunk.toString().trim(),
                                chunkIndex++,
                                startPosition,
                                currentPosition
                        );
                        chunks.add(chunk);
                        
                        int overlapStart = Math.max(0, currentChunk.length() - CHUNK_OVERLAP);
                        currentChunk = new StringBuilder(currentChunk.substring(overlapStart));
                        startPosition = currentPosition - (currentChunk.length());
                    }
                    currentChunk.append(subSegment).append(" ");
                    currentPosition += subSegment.length() + 1;
                }
            } else {
                currentChunk.append(segment).append("\n\n");
                currentPosition += segment.length() + 2;
            }
        }

        // Save the last chunk
        if (currentChunk.length() > 0) {
            CourseChunk chunk = new CourseChunk(
                    course,
                    currentChunk.toString().trim(),
                    chunkIndex,
                    startPosition,
                    currentPosition
            );
            chunks.add(chunk);
        }

        logger.info("Created {} chunks from {} characters of content", chunks.size(), content.length());
        return chunks;
    }

    /**
     * Normalize content by cleaning up whitespace and improving readability.
     */
    private String normalizeContent(String content) {
        // Replace multiple spaces with single space
        String normalized = content.replaceAll("[ \\t]+", " ");
        // Normalize line endings
        normalized = normalized.replaceAll("\\r\\n", "\n");
        // Replace 3+ newlines with double newline
        normalized = normalized.replaceAll("\\n{3,}", "\n\n");
        // Clean up common PDF extraction artifacts
        normalized = normalized.replaceAll("(?m)^\\s+", ""); // Leading whitespace on lines
        return normalized.trim();
    }

    /**
     * Split content into logical segments (paragraphs, sections).
     */
    private List<String> splitIntoSegments(String content) {
        List<String> segments = new ArrayList<>();
        
        // First try splitting by double newlines (paragraphs)
        String[] paragraphs = content.split("\\n\\n+");
        
        // If we only get 1 segment and it's large, try other strategies
        if (paragraphs.length == 1 && content.length() > DEFAULT_CHUNK_SIZE) {
            // Try splitting by single newlines
            paragraphs = content.split("\\n+");
        }
        
        // If still only 1 segment, try splitting by sentences
        if (paragraphs.length == 1 && content.length() > DEFAULT_CHUNK_SIZE) {
            return splitBySentences(content);
        }
        
        for (String para : paragraphs) {
            String trimmed = para.trim();
            if (!trimmed.isEmpty()) {
                segments.add(trimmed);
            }
        }
        
        return segments;
    }

    /**
     * Split content by sentences.
     */
    private List<String> splitBySentences(String content) {
        List<String> sentences = new ArrayList<>();
        // Split by sentence endings (.!?) followed by space or newline
        String[] parts = content.split("(?<=[.!?])\\s+");
        
        StringBuilder currentSentenceGroup = new StringBuilder();
        for (String part : parts) {
            if (currentSentenceGroup.length() + part.length() > DEFAULT_CHUNK_SIZE / 2 && currentSentenceGroup.length() > 0) {
                sentences.add(currentSentenceGroup.toString().trim());
                currentSentenceGroup = new StringBuilder();
            }
            currentSentenceGroup.append(part).append(" ");
        }
        
        if (currentSentenceGroup.length() > 0) {
            sentences.add(currentSentenceGroup.toString().trim());
        }
        
        return sentences.isEmpty() ? List.of(content) : sentences;
    }

    /**
     * Split a large segment into smaller pieces by sentences or fixed size.
     */
    private List<String> splitLargeSegment(String segment) {
        List<String> subSegments = new ArrayList<>();
        
        // First try to split by sentences
        String[] sentences = segment.split("(?<=[.!?])\\s+");
        
        if (sentences.length > 1) {
            StringBuilder current = new StringBuilder();
            for (String sentence : sentences) {
                if (current.length() + sentence.length() > DEFAULT_CHUNK_SIZE - 50 && current.length() > 0) {
                    subSegments.add(current.toString().trim());
                    current = new StringBuilder();
                }
                current.append(sentence).append(" ");
            }
            if (current.length() > 0) {
                subSegments.add(current.toString().trim());
            }
        } else {
            // Fall back to fixed-size splitting
            int pos = 0;
            while (pos < segment.length()) {
                int end = Math.min(pos + DEFAULT_CHUNK_SIZE - 50, segment.length());
                // Try to find a word boundary
                if (end < segment.length()) {
                    int lastSpace = segment.lastIndexOf(' ', end);
                    if (lastSpace > pos) {
                        end = lastSpace;
                    }
                }
                subSegments.add(segment.substring(pos, end).trim());
                pos = end;
            }
        }
        
        return subSegments;
    }

    /**
     * Retrieve relevant chunks for quiz generation.
     * Returns all chunks for comprehensive coverage.
     */
    @Transactional(readOnly = true)
    public List<CourseChunk> retrieveChunks(Long courseId) {
        return chunkRepository.findByCourseIdOrderByChunkIndexAsc(courseId);
    }

    /**
     * Retrieve chunks containing specific keywords.
     */
    @Transactional(readOnly = true)
    public List<CourseChunk> retrieveChunksByKeyword(Long courseId, String keyword) {
        return chunkRepository.findByKeyword(courseId, keyword);
    }

    /**
     * Get the full context for quiz generation.
     * Combines all chunks into a single context string.
     */
    @Transactional(readOnly = true)
    public String getQuizContext(Long courseId) {
        List<CourseChunk> chunks = retrieveChunks(courseId);
        return chunks.stream()
                .map(CourseChunk::getContent)
                .collect(Collectors.joining("\n\n"));
    }

    /**
     * Get sampled context for quiz generation to optimize LLM token usage.
     * Selects a subset of chunks based on the number of questions.
     */
    @Transactional(readOnly = true)
    public List<CourseChunk> getSampledChunks(Long courseId, int numberOfQuestions) {
        List<CourseChunk> allChunks = retrieveChunks(courseId);

        if (allChunks.size() <= numberOfQuestions) {
            return allChunks;
        }

        // Sample chunks evenly distributed across the content
        List<CourseChunk> sampledChunks = new ArrayList<>();
        int step = allChunks.size() / numberOfQuestions;

        for (int i = 0; i < numberOfQuestions && i * step < allChunks.size(); i++) {
            sampledChunks.add(allChunks.get(i * step));
        }

        return sampledChunks;
    }

    /**
     * Check if a course has been indexed.
     */
    @Transactional(readOnly = true)
    public boolean isIndexed(Long courseId) {
        return chunkRepository.countByCourseId(courseId) > 0;
    }

    /**
     * Get the total number of chunks for a course.
     */
    @Transactional(readOnly = true)
    public long getChunkCount(Long courseId) {
        return chunkRepository.countByCourseId(courseId);
    }

    /**
     * Get RAG statistics for a course.
     * Returns a map with chunk count, total characters, and average chunk size.
     */
    @Transactional(readOnly = true)
    public RAGStats getRAGStats(Long courseId) {
        List<CourseChunk> chunks = retrieveChunks(courseId);
        
        if (chunks.isEmpty()) {
            return new RAGStats(0, 0, 0);
        }
        
        int totalChars = chunks.stream()
                .mapToInt(c -> c.getContent().length())
                .sum();
        
        double avgChunkSize = (double) totalChars / chunks.size();
        
        return new RAGStats(chunks.size(), totalChars, avgChunkSize);
    }

    /**
     * RAG statistics record.
     */
    public record RAGStats(int chunkCount, int totalCharacters, double averageChunkSize) {}
}
