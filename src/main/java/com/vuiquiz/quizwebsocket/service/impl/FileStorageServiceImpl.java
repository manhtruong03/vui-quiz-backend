package com.vuiquiz.quizwebsocket.service.impl;

import com.vuiquiz.quizwebsocket.config.FileStorageProperties;
import com.vuiquiz.quizwebsocket.exception.FileStorageException;
import com.vuiquiz.quizwebsocket.exception.MyFileNotFoundException;
import com.vuiquiz.quizwebsocket.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.UUID;
import java.util.Objects;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private final Path fileStorageLocation;

    @Autowired
    public FileStorageServiceImpl(FileStorageProperties fileStorageProperties) {
        this.fileStorageLocation = Paths.get(fileStorageProperties.getUploadDir())
                .toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new FileStorageException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    @Override
    public String storeFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new FileStorageException("Failed to store empty file.");
        }

        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String fileExtension = "";
        try {
            if (originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            // Basic validation for image types for this stage
            if (!fileExtension.matches("\\.(png|jpg|jpeg|gif|webp)$")) {
                throw new FileStorageException("Invalid file type. Only PNG, JPG, JPEG, GIF, WEBP are allowed. Filename: " + originalFilename);
            }

            String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
            Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName);

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }
            return uniqueFileName;
        } catch (IOException ex) {
            throw new FileStorageException("Could not store file " + originalFilename + ". Please try again!", ex);
        } catch (NullPointerException ex) {
            throw new FileStorageException("File original filename is null.", ex);
        }
    }

    @Override
    public Resource loadFileAsResource(String filename) {
        try {
            Path filePath = resolveFilePath(filename);
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new MyFileNotFoundException("File not found or not readable: " + filename);
            }
        } catch (MalformedURLException ex) {
            throw new MyFileNotFoundException("File not found: " + filename, ex);
        }
    }

    @Override
    public Path resolveFilePath(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new MyFileNotFoundException("Filename cannot be null or empty");
        }
        // Security check for path traversal
        if (filename.contains("..")) {
            throw new FileStorageException("Cannot store file with relative path outside current directory " + filename);
        }
        return this.fileStorageLocation.resolve(filename).normalize();
    }

    @Override
    public void deleteFile(String storedFilename) {
        if (!StringUtils.hasText(storedFilename)) {
            throw new FileStorageException("Stored filename for deletion cannot be null or empty.");
        }
        // Prevent path traversal attacks when deleting
        if (storedFilename.contains("..")) {
            throw new FileStorageException("Cannot delete file with relative path outside current directory: " + storedFilename);
        }

        try {
            Path filePath = this.fileStorageLocation.resolve(storedFilename).normalize();
            if (Files.exists(filePath)) { // Check if file exists before attempting to delete
                Files.delete(filePath);
            } else {
                // If the file is already gone, we might not want to throw a hard error,
                // as the goal is to ensure it's not there. Log it.
                // Or, if strict consistency is required, throw MyFileNotFoundException.
                // Let's throw if it was expected to be there based on DB record.
                // However, ImageStorageService will check DB first. If DB record exists but file doesn't,
                // ImageStorageService can log it. Here, if called directly and file not found, it's an issue.
                throw new MyFileNotFoundException("File not found, cannot delete: " + storedFilename);
            }
        } catch (NoSuchFileException ex) {
            throw new MyFileNotFoundException("File not found, cannot delete: " + storedFilename, ex);
        } catch (IOException ex) {
            throw new FileStorageException("Could not delete file: " + storedFilename, ex);
        }
    }
}