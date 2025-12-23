package com.example.demo.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

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
}
