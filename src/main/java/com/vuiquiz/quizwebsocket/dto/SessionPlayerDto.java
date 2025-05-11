// src/main/java/com/vuiquiz/quizwebsocket/dto/SessionPlayerDto.java
package com.vuiquiz.quizwebsocket.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
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
@Schema(description = "Represents the final aggregated results and details for a single player in the session.")
public class SessionPlayerDto {

    @NotBlank(message = "Player client ID cannot be blank.")
    @Schema(description = "The WebSocket client ID used by the player during the session.", example = "dc7dfc5b-daf4-f812-a89b-2d786dc0b70b", requiredMode = Schema.RequiredMode.REQUIRED)
    private String clientId;

    @NotBlank(message = "Player nickname cannot be blank.")
    @Schema(description = "Player's chosen nickname.", example = "Player 01", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nickname;

    @Schema(description = "Optional UUID of the player if they were logged into a user account.", example = "user-uuid-if-any", nullable = true)
    private String userId;

    @NotBlank(message = "Player status cannot be blank.")
    @Schema(description = "Final status of the player (e.g., 'FINISHED', 'KICKED').", example = "FINISHED", requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;

    @NotNull(message = "JoinedAt timestamp cannot be null.")
    @Schema(description = "Unix timestamp (in milliseconds) when the player joined.", example = "1746977241731", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long joinedAt;

    @Schema(description = "Slide index when the player joined (if joined late).", example = "-1", nullable = true)
    private Integer joinSlideIndex;

    @Schema(description = "Unix timestamp (in milliseconds) when player started waiting, if applicable.", nullable = true)
    private Long waitingSince;

    @Schema(description = "Final rank of the player in the session.", example = "1", nullable = true)
    private Integer rank;

    @NotNull(message = "Total score cannot be null.")
    @Schema(description = "Final total score achieved by the player.", example = "2673", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer totalScore;

    @NotNull(message = "Correct answers count cannot be null.")
    @Schema(description = "Total number of correctly answered questions.", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer correctAnswers;

    @NotNull(message = "Streak count cannot be null.")
    @Schema(description = "Maximum answer streak achieved by the player.", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer streakCount;

    @NotNull(message = "Answer count cannot be null.")
    @Schema(description = "Total number of questions answered (correctly or incorrectly).", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer answerCount;

    @NotNull(message = "Unanswered count cannot be null.")
    @Schema(description = "Total number of questions not answered (timeout/skipped).", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer unansweredCount;

    @NotNull(message = "Total time cannot be null.")
    @Schema(description = "Sum of reaction times (in milliseconds) for all answered questions.", example = "14736", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long totalTime;

    @NotNull(message = "Last activity timestamp cannot be null.")
    @Schema(description = "Unix timestamp (in milliseconds) of the player's last activity.", example = "1746977337388", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long lastActivityAt;

    @Schema(description = "Optional JSON object containing information about the player's device.", example = "{\"userAgent\":\"Chrome/100\"}", nullable = true)
    @JsonProperty("deviceInfoJson") // Ensuring JSON property name matches if different from field name
    private JsonNode deviceInfoJson;

    @Schema(description = "Identifier for the player's chosen avatar (e.g., a UUID string or a predefined name).", example = "avatar-cat-uuid-9i8j", nullable = true)
    private String avatarId;
}