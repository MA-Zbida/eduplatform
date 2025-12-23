package com.example.demo.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;

/**
 * Service for handling file uploads and storage.
 * Stores files in a configurable directory and provides retrieval functionality.
 */
@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    private Path fileStorageLocation;

    @PostConstruct
    public void init() {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException ex) {
            throw new RuntimeException("Could not create upload directory: " + uploadDir, ex);
        }
    }

    /**
     * Store a file and return the generated filename.
     *
     * @param file the multipart file to store
     * @return the stored filename (UUID-based)
     */
    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        // Validate file type (PDF only)
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        if (!originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Only PDF files are allowed");
        }

        // Generate unique filename to prevent conflicts
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String storedFilename = UUID.randomUUID().toString() + fileExtension;

        try {
            // Check for path traversal attack
            if (originalFilename.contains("..")) {
                throw new IllegalArgumentException("Invalid file path: " + originalFilename);
            }

            Path targetLocation = this.fileStorageLocation.resolve(storedFilename);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }

            return storedFilename;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + originalFilename, ex);
        }
    }

    /**
     * Load a file as a Resource.
     *
     * @param filename the filename to load
     * @return the file as a Resource
     */
    public Resource loadFileAsResource(String filename) {
        try {
            Path filePath = this.fileStorageLocation.resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("File not found: " + filename);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("File not found: " + filename, ex);
        }
    }

    /**
     * Delete a file.
     *
     * @param filename the filename to delete
     * @return true if deleted successfully
     */
    public boolean deleteFile(String filename) {
        if (filename == null || filename.isBlank()) {
            return false;
        }

        try {
            Path filePath = this.fileStorageLocation.resolve(filename).normalize();
            return Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            throw new RuntimeException("Could not delete file: " + filename, ex);
        }
    }

    /**
     * Get the file storage location path.
     *
     * @return the path to the storage directory
     */
    public Path getFileStorageLocation() {
        return fileStorageLocation;
    }

    /**
     * Check if a file exists.
     *
     * @param filename the filename to check
     * @return true if the file exists
     */
    public boolean fileExists(String filename) {
        if (filename == null || filename.isBlank()) {
            return false;
        }
        Path filePath = this.fileStorageLocation.resolve(filename).normalize();
        return Files.exists(filePath);
    }

    /**
     * Extract text content from a PDF file.
     * Uses enhanced extraction settings for better handling of layouts and formulas.
     *
     * @param filename the PDF filename to extract text from
     * @return the extracted text content, or empty string if extraction fails
     */
    public String extractTextFromPdf(String filename) {
        if (filename == null || filename.isBlank()) {
            return "";
        }

        Path filePath = this.fileStorageLocation.resolve(filename).normalize();
        
        if (!Files.exists(filePath)) {
            logger.warn("PDF file not found for text extraction: {}", filename);
            return "";
        }

        try (PDDocument document = Loader.loadPDF(filePath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            
            // Configure stripper for better text extraction
            stripper.setSortByPosition(true);
            stripper.setAddMoreFormatting(true);
            
            // Add paragraph breaks between sections
            stripper.setParagraphStart("\n");
            stripper.setParagraphEnd("\n\n");
            stripper.setPageStart("\n");
            stripper.setPageEnd("\n\n");
            stripper.setLineSeparator("\n");
            
            // Extract text from all pages
            String text = stripper.getText(document);
            
            // Post-process the text to improve readability
            text = postProcessPdfText(text);
            
            logger.info("Extracted {} characters from PDF ({} pages): {}", 
                    text.length(), document.getNumberOfPages(), filename);
            return text;
        } catch (IOException e) {
            logger.error("Failed to extract text from PDF: {}", filename, e);
            return "";
        }
    }

    /**
     * Post-process extracted PDF text to improve readability.
     * Handles common PDF extraction artifacts and improves chunking compatibility.
     */
    private String postProcessPdfText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        // Remove excessive whitespace while preserving paragraph structure
        text = text.replaceAll("[ \\t]+", " ");  // Multiple spaces to single space
        text = text.replaceAll("(?m)^[ \\t]+", "");  // Leading whitespace on lines
        text = text.replaceAll("(?m)[ \\t]+$", "");  // Trailing whitespace on lines
        
        // Normalize line endings
        text = text.replaceAll("\\r\\n", "\n");
        text = text.replaceAll("\\r", "\n");
        
        // Convert 3+ newlines to double newlines (paragraph breaks)
        text = text.replaceAll("\\n{3,}", "\n\n");
        
        // Try to fix broken sentences (line ending without punctuation)
        // This helps with PDFs that have hard line breaks mid-sentence
        text = text.replaceAll("(?<![.!?:;,\\-])\\n(?=[a-z])", " ");
        
        // Clean up common PDF artifacts
        text = text.replaceAll("\\u0000", "");  // Null characters
        text = text.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");  // Control characters
        
        // Improve formula readability - add spacing around common operators
        text = text.replaceAll("([a-zA-Z])([=<>≤≥≠+−×÷])([a-zA-Z0-9])", "$1 $2 $3");
        
        return text.trim();
    }

    /**
     * Get the full path to a stored file.
     *
     * @param filename the filename
     * @return the full path to the file
     */
    public Path getFilePath(String filename) {
        return this.fileStorageLocation.resolve(filename).normalize();
    }
}
