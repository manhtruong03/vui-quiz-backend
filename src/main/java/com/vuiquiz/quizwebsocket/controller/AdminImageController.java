package com.vuiquiz.quizwebsocket.controller;

import com.vuiquiz.quizwebsocket.dto.ImageStorageAdminViewDTO;
import com.vuiquiz.quizwebsocket.dto.ImageStorageUpdateDTO;
import com.vuiquiz.quizwebsocket.exception.FileStorageException;
import com.vuiquiz.quizwebsocket.exception.ResourceNotFoundException;
import com.vuiquiz.quizwebsocket.exception.StorageQuotaExceededException;
import com.vuiquiz.quizwebsocket.model.ImageStorage;
import com.vuiquiz.quizwebsocket.payload.response.MessageResponse;
import com.vuiquiz.quizwebsocket.security.services.UserDetailsImpl;
import com.vuiquiz.quizwebsocket.service.FileStorageService;
import com.vuiquiz.quizwebsocket.service.ImageStorageService;
import com.vuiquiz.quizwebsocket.service.UserAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/images")
@Tag(name = "Admin Image Management", description = "APIs for administrators to manage images")
@CrossOrigin(origins = "*", maxAge = 3600) // Adjust for production
@RequiredArgsConstructor
@Slf4j
public class AdminImageController {

    private final ImageStorageService imageStorageService;
    private final UserAccountService userAccountService;
    private final FileStorageService fileStorageService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all images (paginated)",
            description = "Retrieves a paginated list of all image records available in the system. Requires ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Successfully retrieved list of images.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Page.class))) // Schema for Page<ImageStorageAdminViewDTO>
    @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token is missing or invalid.")
    @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role.")
    public ResponseEntity<Page<ImageStorageAdminViewDTO>> getAllImages(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            @Parameter(description = "Pagination and sorting parameters (e.g., page=0&size=10&sort=createdAt,desc)")
            Pageable pageable) {
        log.info("Admin request to list all images, pageable: {}", pageable);
        Page<ImageStorageAdminViewDTO> imageRecords = imageStorageService.getAllImageRecords(pageable);
        return ResponseEntity.ok(imageRecords);
    }

    @GetMapping("/{imageId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get image details by ID",
            description = "Retrieves detailed information for a specific image record by its UUID. Requires ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Successfully retrieved image details.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ImageStorageAdminViewDTO.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token is missing or invalid.")
    @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role.")
    @ApiResponse(responseCode = "404", description = "Image record not found with the given UUID.")
    public ResponseEntity<ImageStorageAdminViewDTO> getImageById(
            @Parameter(description = "UUID of the image record to retrieve.", required = true, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
            @PathVariable UUID imageId) {
        log.info("Admin request to get image by ID: {}", imageId);
        Optional<ImageStorageAdminViewDTO> dtoOptional = imageStorageService.getImageRecordById(imageId);
        return dtoOptional
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{imageId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete an image by ID",
            description = "Deletes an image record from the database and its corresponding physical file from storage. Updates the original creator's storage quota. Requires ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Image deleted successfully.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = MessageResponse.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token is missing or invalid.")
    @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role.")
    @ApiResponse(responseCode = "404", description = "Image record not found with the given UUID.")
    @ApiResponse(responseCode = "500", description = "Internal server error during deletion process.")
    public ResponseEntity<?> deleteImage(
            @Parameter(description = "UUID of the image record to delete.", required = true, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
            @PathVariable UUID imageId) {
        log.info("Admin request to delete image by ID: {}", imageId);
        try {
            // It's good practice to fetch the creatorId before deleting the record,
            // as the deleteImageStorageAndFile might remove the record making it unavailable.
            // However, the current ImageStorageService.deleteImageStorageAndFile returns creatorId effectively by fetching before deleting.
            // For clarity, let's fetch it here or ensure service method guarantees it.
            // Your current service impl fetches before delete, which is fine.

            ImageStorage imageDetails = imageStorageService.getImageStorageById(imageId)
                    .orElseThrow(() -> new ResourceNotFoundException("ImageStorage", "id", imageId));
            UUID creatorId = imageDetails.getCreatorId();

            long freedSpace = imageStorageService.deleteImageStorageAndFile(imageId);
            log.info("Image record {} and its file deleted. Freed space: {} bytes.", imageId, freedSpace);

            if (creatorId != null && freedSpace > 0) {
                try {
                    userAccountService.updateUserStorageUsed(creatorId, -freedSpace);
                    log.info("Successfully updated storage for user {} by {} bytes.", creatorId, -freedSpace);
                } catch (ResourceNotFoundException e) {
                    log.warn("User {} not found when trying to update storage after image {} deletion. Message: {}", creatorId, imageId, e.getMessage());
                    // Continue with image deletion success, but log this issue.
                } catch (Exception e) {
                    log.error("Failed to update storage for user {} after image {} deletion. Freed space was {}. Error: {}", creatorId, imageId, freedSpace, e.getMessage(), e);
                    // This is a critical situation for data consistency.
                    // Depending on requirements, you might want to return a specific error or mark for reconciliation.
                    // For now, we'll return success for image deletion but log this error.
                }
            } else if (freedSpace == 0) {
                log.info("No space was freed (file might have been 0 bytes or size unknown) for image {}.", imageId);
            }


            return ResponseEntity.ok(new MessageResponse("Image " + imageId + " deleted successfully."));

        } catch (ResourceNotFoundException e) {
            log.warn("Failed to delete image ID {}: {}", imageId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse(e.getMessage()));
        } catch (FileStorageException e) {
            log.error("File storage error during deletion of image ID {}: {}", imageId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Error deleting image file: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during deletion of image ID {}: {}", imageId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("An unexpected error occurred while deleting the image."));
        }
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Upload a new image by an administrator",
            description = "Allows an administrator to upload an image. The image is stored, a database record is created, " +
                    "and the specified creator's (or admin's if not specified) storage quota is updated. " +
                    "Requires ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Multipart request containing the image file and optional creator ID.",
            required = true
    )
    @ApiResponse(responseCode = "201", description = "Image uploaded successfully and record created.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ImageStorageAdminViewDTO.class)))
    @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input (e.g., no file, invalid file type, invalid creatorId format, creatorId not found).",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = MessageResponse.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token is missing or invalid.")
    @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role.")
    @ApiResponse(responseCode = "413", description = "Payload Too Large - Upload exceeds storage quota.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = MessageResponse.class)))
    @ApiResponse(responseCode = "500", description = "Internal Server Error - Unexpected error during processing.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = MessageResponse.class)))
    public ResponseEntity<?> uploadImageByAdmin(
            @Parameter(description = "The image file to upload.", required = true)
            @RequestParam("imageFile") MultipartFile imageFile,
            @Parameter(description = "(Optional) UUID of the user to be credited with the upload. If omitted, the uploading admin is credited.")
            @RequestParam(name = "creatorId", required = false) String creatorIdString,
            Authentication authentication) {

        log.info("Admin request to upload an image. Original filename: {}", imageFile.getOriginalFilename());

        if (imageFile == null || imageFile.isEmpty()) {
            log.warn("Upload attempt failed: No file provided.");
            return ResponseEntity.badRequest().body(new MessageResponse("Please select a file to upload."));
        }

        UserDetailsImpl adminUserDetails = (UserDetailsImpl) authentication.getPrincipal();
        UUID adminUserId = adminUserDetails.getId();
        UUID finalCreatorId;

        if (creatorIdString != null && !creatorIdString.trim().isEmpty()) {
            try {
                UUID parsedCreatorId = UUID.fromString(creatorIdString);
                // Validate if the provided creatorId exists
                if (!userAccountService.getUserById(parsedCreatorId).isPresent()) {
                    log.warn("Upload attempt failed: Specified creatorId {} not found.", parsedCreatorId);
                    return ResponseEntity.badRequest().body(new MessageResponse("Specified creatorId not found."));
                }
                finalCreatorId = parsedCreatorId;
                log.info("Image will be credited to specified user: {}", finalCreatorId);
            } catch (IllegalArgumentException e) {
                log.warn("Upload attempt failed: Invalid creatorId format provided: {}", creatorIdString);
                return ResponseEntity.badRequest().body(new MessageResponse("Invalid creatorId format. Must be a UUID."));
            }
        } else {
            finalCreatorId = adminUserId;
            log.info("Image will be credited to uploading admin: {}", finalCreatorId);
        }

        try {
            // 1. Check storage quota
            if (!userAccountService.canUserUpload(finalCreatorId, imageFile.getSize())) {
                log.warn("Upload failed for user {}: Storage quota exceeded for file size {}.", finalCreatorId, imageFile.getSize());
                throw new StorageQuotaExceededException("Upload exceeds storage quota for user " + finalCreatorId +
                        ". Required: " + imageFile.getSize() + " bytes.");
            }

            // 2. Store the file physically
            String storedFileName = fileStorageService.storeFile(imageFile);
            log.info("File stored successfully as: {} by user {}", storedFileName, adminUserId);

            // 3. Create ImageStorage record and get DTO (using the new service method)
            ImageStorageAdminViewDTO imageDto = imageStorageService.createImageRecordAndGetDTO(imageFile, storedFileName, finalCreatorId);
            log.info("Image record created successfully with ID: {}", imageDto.getImageId());

            // 4. Update user's storage used
            userAccountService.updateUserStorageUsed(finalCreatorId, imageFile.getSize());
            log.info("Storage updated for user {} by {} bytes.", finalCreatorId, imageFile.getSize());

            return ResponseEntity.status(HttpStatus.CREATED).body(imageDto);

        } catch (StorageQuotaExceededException e) {
            log.warn("Storage quota exceeded for user {}: {}", finalCreatorId, e.getMessage());
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(new MessageResponse(e.getMessage()));
        } catch (FileStorageException e) {
            log.error("File storage error during admin upload by {}: {}", adminUserId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Error storing file: " + e.getMessage()));
        } catch (ResourceNotFoundException e) { // Should be caught earlier if creatorId validation is strict
            log.error("Resource not found during admin upload by {}: {}", adminUserId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse(e.getMessage()));
        }
        catch (Exception e) {
            log.error("Unexpected error during admin image upload by {}: {}", adminUserId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("An unexpected error occurred during image upload."));
        }
    }

    @PutMapping("/{imageId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update image metadata by ID",
            description = "Allows an administrator to update metadata of an existing image record, such as its original filename. " +
                    "Only non-null fields in the request body that represent a change will be processed. Requires ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Image metadata updated successfully.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ImageStorageAdminViewDTO.class)))
    @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input data (e.g., validation error).",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = MessageResponse.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token is missing or invalid.")
    @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role.")
    @ApiResponse(responseCode = "404", description = "Image record not found with the given UUID.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = MessageResponse.class)))
    @ApiResponse(responseCode = "500", description = "Internal Server Error - Unexpected error during processing.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = MessageResponse.class)))
    public ResponseEntity<?> updateImageMetadata(
            @Parameter(description = "UUID of the image record to update.", required = true, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
            @PathVariable UUID imageId,
            @Parameter(description = "JSON object containing the image metadata fields to update. " +
                    "Only 'originalFileName' is supported in this version. " +
                    "Provide a non-blank string to update, or omit/null to keep existing.",
                    required = true)
            @Valid @RequestBody ImageStorageUpdateDTO updateDTO) {

        log.info("Admin request to update metadata for image ID: {}", imageId);
        try {
            ImageStorageAdminViewDTO updatedImageDTO = imageStorageService.updateImageMetadata(imageId, updateDTO);
            return ResponseEntity.ok(updatedImageDTO);
        } catch (ResourceNotFoundException e) {
            log.warn("Failed to update metadata for image ID {}: {}", imageId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse(e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid argument while updating metadata for image ID {}: {}", imageId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during metadata update for image ID {}: {}", imageId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("An unexpected error occurred while updating image metadata."));
        }
    }
}