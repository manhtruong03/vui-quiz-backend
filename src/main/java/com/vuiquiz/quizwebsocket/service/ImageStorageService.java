package com.vuiquiz.quizwebsocket.service;

import com.vuiquiz.quizwebsocket.dto.ImageStorageAdminViewDTO;
import com.vuiquiz.quizwebsocket.dto.ImageStorageUpdateDTO;
import com.vuiquiz.quizwebsocket.exception.FileStorageException;
import com.vuiquiz.quizwebsocket.exception.ResourceNotFoundException;
import com.vuiquiz.quizwebsocket.model.ImageStorage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile; // Add this import

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ImageStorageService {
    // Existing methods (confirm they align or remove if findOrCreateByFilePath covers them)
     ImageStorage createImageStorage(ImageStorage imageStorage); // This is generic, let's make a more specific one

    /**
     * Creates an ImageStorage record in the database for a newly uploaded file.
     *
     * @param originalFile The uploaded MultipartFile.
     * @param storedFilename The unique filename under which the file is stored on the server.
     * @param creatorId The ID of the user who uploaded the file.
     * @return The persisted ImageStorage entity.
     */
    ImageStorage createImageRecord(MultipartFile originalFile, String storedFilename, UUID creatorId);

    Optional<ImageStorage> getImageStorageById(UUID imageId);
    Optional<ImageStorage> getImageByFilePath(String storedFilePath); // Ensure this refers to the unique stored filename
    List<ImageStorage> getImagesByCreatorId(UUID creatorId);
    Page<ImageStorage> getAllImageStorages(Pageable pageable);
    ImageStorage updateImageStorage(UUID imageId, ImageStorage imageDetails);
    void deleteImageStorage(UUID imageId); // This should also trigger file deletion from disk in later stages

    // This is useful, keep it. It might be used by QuizService later if image URLs are passed directly.
    // For new uploads, createImageRecord is more direct.
    ImageStorage findOrCreateByFilePath(String filePath, UUID creatorId);

    /**
     * Generates a public URL for accessing the stored image.
     *
     * @param imageStorage The ImageStorage entity.
     * @return The fully qualified public URL.
     */
    String getPublicUrl(ImageStorage imageStorage);

    /**
     * Generates a public URL for accessing the stored image by its stored filename.
     *
     * @param storedFilename The unique filename as stored on the server.
     * @return The fully qualified public URL.
     */
    String getPublicUrl(String storedFilename);

    /**
     * Generates a public URL for an image by its ImageStorage ID.
     * @param imageId The UUID of the ImageStorage record.
     * @return The public URL, or null if the image is not found.
     */
    String getPublicUrl(UUID imageId);

    /**
     * Deletes an ImageStorage record and its corresponding physical file.
     *
     * @param imageId The ID of the ImageStorage record to delete.
     * @return The size of the deleted file in bytes. Returns 0 if the record or file was not found or if size is unknown.
     * @throws ResourceNotFoundException if the ImageStorage record is not found.
     * @throws FileStorageException if an error occurs during physical file deletion.
     */
    long deleteImageStorageAndFile(UUID imageId);

    /**
     * Retrieves a paginated list of all image records, mapped to ImageStorageAdminViewDTO.
     *
     * @param pageable Pagination and sorting information.
     * @return A page of ImageStorageAdminViewDTOs.
     */
    Page<ImageStorageAdminViewDTO> getAllImageRecords(Pageable pageable);

    /**
     * Retrieves details of a specific image record by its ID, mapped to ImageStorageAdminViewDTO.
     *
     * @param imageId The UUID of the image record.
     * @return An Optional containing the ImageStorageAdminViewDTO if found, or an empty Optional otherwise.
     */
    Optional<ImageStorageAdminViewDTO> getImageRecordById(UUID imageId);

    /**
     * Stores an image file, creates its record in the database, and returns its admin view DTO.
     *
     * @param originalFile The uploaded MultipartFile.
     * @param storedFilename The unique filename under which the file is stored on the server.
     * @param creatorId The ID of the user who is credited for the upload.
     * @return ImageStorageAdminViewDTO of the created image record.
     */
    ImageStorageAdminViewDTO createImageRecordAndGetDTO(MultipartFile originalFile, String storedFilename, UUID creatorId);

    // (Existing method signatures from previous phases)

    /**
     * Updates the metadata of an existing image record.
     * Currently supports updating the original filename.
     *
     * @param imageId The UUID of the image record to update.
     * @param updateDTO DTO containing the metadata fields to update.
     * @return ImageStorageAdminViewDTO of the updated image record.
     * @throws ResourceNotFoundException if the image record is not found.
     * @throws IllegalArgumentException if update parameters are invalid.
     */
    ImageStorageAdminViewDTO updateImageMetadata(UUID imageId, ImageStorageUpdateDTO updateDTO);
}