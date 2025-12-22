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

    public RAGService(CourseChunkRepository chunkRepository) {
        this.chunkRepository = chunkRepository;
    }

    /**
     * Index a course by chunking its content.
     * This prepares the content for RAG-based retrieval.
     */
    public void indexCourse(Course course) {
        logger.info("Starting RAG indexing for course: {}", course.getId());

        // Clear existing chunks
        chunkRepository.deleteByCourseId(course.getId());

        // Chunk the content
        List<CourseChunk> chunks = chunkContent(course, course.getContent());

        // Save chunks
        chunkRepository.saveAll(chunks);

        logger.info("Indexed {} chunks for course: {}", chunks.size(), course.getId());
    }

    /**
     * Chunk course content into smaller, overlapping segments.
     */
    private List<CourseChunk> chunkContent(Course course, String content) {
        List<CourseChunk> chunks = new ArrayList<>();

        if (content == null || content.isBlank()) {
            return chunks;
        }

        // Split by paragraphs first, then combine into chunks
        String[] paragraphs = content.split("\\n\\n+");
        StringBuilder currentChunk = new StringBuilder();
        int chunkIndex = 0;
        int startPosition = 0;
        int currentPosition = 0;

        for (String paragraph : paragraphs) {
            // If adding this paragraph exceeds chunk size, save current chunk
            if (currentChunk.length() + paragraph.length() > DEFAULT_CHUNK_SIZE && currentChunk.length() > 0) {
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

            currentChunk.append(paragraph).append("\n\n");
            currentPosition += paragraph.length() + 2;
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

        return chunks;
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
}
