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
        // Consider validation for file path uniqueness if not handled by DB constraint message
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
        // Typically, you might only update metadata like fileName if filePath is immutable
        existingImage.setFileName(imageDetails.getFileName());
        // existingImage.setFilePath(imageDetails.getFilePath()); // Be careful with this
        return imageStorageRepository.save(existingImage);
    }

    @Override
    @Transactional
    public void deleteImageStorage(UUID imageId) {
        ImageStorage image = imageStorageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("ImageStorage", "id", imageId));
        // Consider actual file system cleanup logic here or in an event listener
        imageStorageRepository.delete(image);
    }
}
