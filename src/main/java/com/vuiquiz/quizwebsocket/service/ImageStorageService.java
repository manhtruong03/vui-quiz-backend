package com.vuiquiz.quizwebsocket.service;

import com.vuiquiz.quizwebsocket.model.ImageStorage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ImageStorageService {
    ImageStorage createImageStorage(ImageStorage imageStorage);
    Optional<ImageStorage> getImageStorageById(UUID imageId);
    Optional<ImageStorage> getImageByFilePath(String filePath);
    List<ImageStorage> getImagesByCreatorId(UUID creatorId);
    Page<ImageStorage> getAllImageStorages(Pageable pageable);
    ImageStorage updateImageStorage(UUID imageId, ImageStorage imageDetails); // Limited updates likely
    void deleteImageStorage(UUID imageId);
}
