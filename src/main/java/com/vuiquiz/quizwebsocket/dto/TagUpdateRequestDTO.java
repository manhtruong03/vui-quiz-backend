package com.vuiquiz.quizwebsocket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request DTO for updating an existing Tag by an administrator.")
public class TagUpdateRequestDTO {

    @Size(min = 1, max = 100, message = "Tag name must be between 1 and 100 characters if provided.")
    @Schema(description = "The new name of the tag. If null or blank, the name will not be changed.", example = "Advanced Programming")
    private String name;

    @Size(max = 255, message = "Tag description cannot exceed 255 characters if provided.")
    @Schema(description = "The new optional description for the tag. If null, the description will not be changed.", example = "Tags for advanced programming topics.")
    private String description;
}