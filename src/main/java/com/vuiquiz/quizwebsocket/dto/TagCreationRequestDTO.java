package com.vuiquiz.quizwebsocket.dto;

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
@Schema(description = "Request DTO for creating a new Tag by an administrator.")
public class TagCreationRequestDTO {

    @NotBlank(message = "Tag name cannot be blank.")
    @Size(min = 1, max = 100, message = "Tag name must be between 1 and 100 characters.")
    @Schema(description = "The name of the tag.", example = "Programming", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 255, message = "Tag description cannot exceed 255 characters.")
    @Schema(description = "Optional description for the tag.", example = "Tags related to programming languages and concepts.")
    private String description;
}