// src/main/java/com/vuiquiz/quizwebsocket/dto/SessionPlayerAnswerDto.java
package com.vuiquiz.quizwebsocket.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode; // For choice as potential array and usedPowerUpContext
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Represents a specific answer by a player for a specific game slide.")
public class SessionPlayerAnswerDto {

    @NotBlank(message = "Player answer client ID cannot be blank.")
    @Schema(description = "Player's 'cid', used by backend to link to the correct player_id.", example = "dc7dfc5b-daf4-f812-a89b-2d786dc0b70b", requiredMode = Schema.RequiredMode.REQUIRED)
    private String clientId;

    @NotNull(message = "Question index cannot be null.")
    @Schema(description = "0-based index of the game slide this answer pertains to (used for frontend reference, backend links via slideId).", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer questionIndex; // Corresponds to slideIndex in the context of gameSlides array

    @Schema(description = "Raw choice index(es) selected by the player. Can be a single number, an array of numbers (for jumble/multiple select), or null.",
            example = "0", additionalProperties = Schema.AdditionalPropertiesValue.TRUE, nullable = true)
    private JsonNode choice; // Using JsonNode to handle number | number[] | null flexibility

    @Schema(description = "Raw text input from the player, if applicable (e.g., for open-ended questions).", example = "Paris", nullable = true)
    private String text;

    @NotNull(message = "Reaction time cannot be null.")
    @Schema(description = "Time taken by the player to submit the answer, in milliseconds.", example = "2630", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer reactionTimeMs;

    @NotNull(message = "Answer timestamp cannot be null.")
    @Schema(description = "Unix timestamp (in milliseconds) when the answer was submitted.", example = "1746977279627", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long answerTimestamp;

    @NotBlank(message = "Answer status cannot be blank.")
    @Schema(description = "Status of the answer (e.g., 'CORRECT', 'WRONG', 'TIMEOUT', 'SKIPPED').", example = "CORRECT", requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;

    @NotNull(message = "Base points cannot be null.")
    @Schema(description = "Points awarded for the answer before any multipliers or power-ups.", example = "956", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer basePoints;

    @NotNull(message = "Final points cannot be null.")
    @Schema(description = "Final points awarded for the answer after all calculations.", example = "956", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer finalPoints;

    @Schema(description = "Optional UUID of the power-up used by the player for this answer.", example = "power-up-uuid", nullable = true)
    private String usedPowerUpId;

    @Schema(description = "Optional JSON object detailing the context or effect of the used power-up.", example = "{\"effect\":\"doubled_points\"}", nullable = true)
    @JsonProperty("usedPowerUpContextJson") // Explicit to match TypeScript if casing differs.
    private JsonNode usedPowerUpContextJson;
}