// src/main/java/com/vuiquiz/quizwebsocket/dto/SessionGameSlideDto.java
package com.vuiquiz.quizwebsocket.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Represents the state and data of a single slide shown during the game session.")
public class SessionGameSlideDto {

    @NotNull(message = "Slide index cannot be null.")
    @Schema(description = "0-based index of this slide in the game sequence.", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer slideIndex;

    @NotBlank(message = "Slide type cannot be blank.")
    @Schema(description = "Type of the slide (e.g., 'QUESTION_SLIDE', 'CONTENT_SLIDE', 'LEADERBOARD').", example = "CONTENT_SLIDE", requiredMode = Schema.RequiredMode.REQUIRED)
    private String slideType;

    @NotBlank(message = "Slide status cannot be blank.")
    @Schema(description = "Final status of the slide (e.g., 'ENDED', 'SKIPPED').", example = "ENDED", requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;

    @Schema(description = "Unix timestamp (in milliseconds) when this slide started being displayed.", example = "1746977275478", nullable = true)
    private Long startedAt;

    @Schema(description = "Unix timestamp (in milliseconds) when this slide finished being displayed.", example = "1746977276996", nullable = true)
    private Long endedAt;

    @Schema(description = "Optional UUID of the original question from the quiz definition, if this slide was a question.", example = "4188076c-c648-4e7a-9bb0-fe8e5e5ce1a4", nullable = true)
    private String originalQuestionId;

    @Schema(description = "Snapshot of the question data as it was distributed or shown during the game. Structure depends on the question type.", nullable = true)
    @JsonProperty("questionDistributionJson")
    private JsonNode questionDistributionJson;

    @Valid
    @NotNull(message = "Player answers list cannot be null (can be empty if no one answered or not a question slide).")
    @Schema(description = "Array of answers submitted by players for this specific slide. Empty for non-question slides or if no answers were given.")
    private List<SessionPlayerAnswerDto> playerAnswers;
}