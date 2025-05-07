package com.vuiquiz.quizwebsocket.service.impl;

import com.vuiquiz.quizwebsocket.exception.ResourceNotFoundException;
import com.vuiquiz.quizwebsocket.model.ImageStorage;
import com.vuiquiz.quizwebsocket.repository.ImageStorageRepository;
import com.vuiquiz.quizwebsocket.service.ImageStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils; // For StringUtils.getFilenameExtension

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ImageStorageServiceImpl implements ImageStorageService {

    private final ImageStorageRepository imageStorageRepository;

    @Autowired
    public ImageStorageServiceImpl(ImageStorageRepository imageStorageRepository) {
        this.imageStorageRepository = imageStorageRepository;
    }

    @Override
    @Transactional
    public ImageStorage createImageStorage(ImageStorage imageStorage) {
        imageStorage.setImageId(null);
        return imageStorageRepository.save(imageStorage);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ImageStorage> getImageStorageById(UUID imageId) {
        return imageStorageRepository.findById(imageId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ImageStorage> getImageByFilePath(String filePath) {
        return imageStorageRepository.findByFilePath(filePath);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ImageStorage> getImagesByCreatorId(UUID creatorId) {
        return imageStorageRepository.findByCreatorId(creatorId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ImageStorage> getAllImageStorages(Pageable pageable) {
        return imageStorageRepository.findAll(pageable);
    }

    @Override
    @Transactional
    public ImageStorage updateImageStorage(UUID imageId, ImageStorage imageDetails) {
        ImageStorage existingImage = imageStorageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("ImageStorage", "id", imageId));
        existingImage.setFileName(imageDetails.getFileName());
        // existingImage.setFilePath(imageDetails.getFilePath()); // Be careful with this
        return imageStorageRepository.save(existingImage);
    }

    @Override
    @Transactional
    public void deleteImageStorage(UUID imageId) {
        ImageStorage image = imageStorageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("ImageStorage", "id", imageId));
        imageStorageRepository.delete(image);
    }

    @Override
    @Transactional
    public ImageStorage findOrCreateByFilePath(String filePath, UUID creatorId) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return null; // Or throw IllegalArgumentException
        }
        Optional<ImageStorage> existingImage = imageStorageRepository.findByFilePath(filePath);
        if (existingImage.isPresent()) {
            return existingImage.get();
        } else {
            // Basic filename extraction (can be improved)
            String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
            // Basic content type (can be improved or set to a generic default)
            String extension = StringUtils.getFilenameExtension(fileName);
            String contentType = "image/" + (extension != null ? extension : "jpeg"); // Default to jpeg if no extension

            ImageStorage newImage = ImageStorage.builder()
                    .filePath(filePath)
                    .creatorId(creatorId)
                    .fileName(fileName)
                    .contentType(contentType)
                    .fileSize(0L) // Placeholder, actual size not known from URL
                    .build();
            return imageStorageRepository.save(newImage);
        }
    }
}