package com.vuiquiz.quizwebsocket.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull; // Ensure this is from jakarta.validation
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Represents an answer choice for a question")
public class ChoiceDTO {

    @Schema(description = "Text content of the answer choice. Used if 'image' is not present for this choice.", example = "True")
    private String answer;

    @Schema(description = "Image content of the answer choice. Used if 'answer' text is not present for this choice.")
    private ImageDetailDTO image; // This should be @Valid if you expect validation on nested objects during request processing

    @NotNull(message = "Correctness flag must be provided for a choice.")
    @Schema(description = "Indicates if this choice is a correct answer.", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean correct;
}