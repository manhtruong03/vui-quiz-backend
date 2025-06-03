package com.vuiquiz.quizwebsocket.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Data Transfer Object for updating an ImageStorage record's metadata by an administrator.")
public class ImageStorageUpdateDTO {

    @Schema(description = "The new original filename for the image. " +
            "If provided and not blank, the existing filename will be updated. " +
            "If null or blank, the filename will not be changed.",
            example = "new_image_filename.jpg", nullable = true)
    @Size(max = 255, message = "Original filename cannot exceed 255 characters.")
    // Not using @NotBlank, as we want to allow submitting null/blank to indicate no change for this field.
    // Validation for non-empty string if provided can be done in service or controller if needed.
    private String originalFileName;

    // Add other updatable fields here in future phases, e.g.:
    // private String altText;
}