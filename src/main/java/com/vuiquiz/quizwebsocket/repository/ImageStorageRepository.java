package com.vuiquiz.quizwebsocket.repository;

import com.vuiquiz.quizwebsocket.model.ImageStorage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ImageStorageRepository extends JpaRepository<ImageStorage, UUID> {
    Optional<ImageStorage> findByFilePath(String filePath);
    List<ImageStorage> findByCreatorId(UUID creatorId);
    List<ImageStorage> findByContentTypeStartingWith(String contentTypePrefix); // e.g. "image/"
}