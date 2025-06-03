package com.vuiquiz.quizwebsocket.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Data Transfer Object for viewing ImageStorage details by an administrator.")
public class ImageStorageAdminViewDTO {

    @Schema(description = "Unique identifier of the image record.", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
    private UUID imageId;

    @Schema(description = "Original filename of the uploaded image.", example = "profile_picture.jpg")
    private String originalFileName;

    @Schema(description = "Stored filename (unique identifier) of the image on the server.", example = "unique-id-123.jpg")
    private String storedFileName;

    @Schema(description = "Publicly accessible URL for the image.", example = "http://localhost:8080/files/images/unique-id-123.jpg")
    private String publicUrl;

    @Schema(description = "Content type of the image.", example = "image/jpeg")
    private String contentType;

    @Schema(description = "Size of the image file in bytes.", example = "102400")
    private Long fileSize;

    @Schema(description = "UUID of the user who created/uploaded the image.", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479", nullable = true)
    private UUID creatorId;

    @Schema(description = "Username of the user who created/uploaded the image.", example = "admin_user", nullable = true)
    private String creatorUsername;

    @Schema(description = "Timestamp of when the image record was created.")
    private OffsetDateTime createdAt;

    @Schema(description = "Timestamp of the last update to the image record.")
    private OffsetDateTime updatedAt;
}