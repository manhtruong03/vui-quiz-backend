// src/main/java/com/vuiquiz/quizwebsocket/dto/SessionFinalizationDto.java
package com.vuiquiz.quizwebsocket.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Main DTO containing all data for finalizing a game session and storing its results.")
public class SessionFinalizationDto {

    @NotBlank(message = "Game PIN cannot be blank.")
    @Schema(description = "The game PIN of the session.", example = "195970", requiredMode = Schema.RequiredMode.REQUIRED)
    private String gamePin;

    @NotBlank(message = "Quiz ID cannot be blank.")
    @Schema(description = "UUID of the quiz that was played.", example = "6b14fbae-7e64-4a38-a7ee-5c56c1224825", requiredMode = Schema.RequiredMode.REQUIRED)
    private String quizId;

    @NotBlank(message = "Host User ID cannot be blank.")
    @Schema(description = "User ID of the host who ran the session. If invalid, backend will use authenticated user.", example = "host-1746977232792", requiredMode = Schema.RequiredMode.REQUIRED)
    private String hostUserId;

    @Schema(description = "Unix timestamp (in milliseconds) when the session started.", example = "1746977232795", nullable = true)
    private Long sessionStartTime;

    @Schema(description = "Unix timestamp (in milliseconds) when the session ended.", example = "1746977337440", nullable = true)
    private Long sessionEndTime;

    @NotBlank(message = "Game type cannot be blank.")
    @Schema(description = "Type of game session (e.g., 'LIVE', 'ASSIGNMENT').", example = "LIVE", requiredMode = Schema.RequiredMode.REQUIRED)
    private String gameType;

    @NotNull(message = "Final player count cannot be null.")
    @PositiveOrZero(message = "Final player count must be zero or positive.")
    @Schema(description = "The final count of players in the session.", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer finalPlayerCount;

    @NotBlank(message = "Final session status cannot be blank.")
    @Schema(description = "The final status of the session (e.g., 'LOBBY', 'RUNNING', 'ENDED', 'ABORTED').", example = "ENDED", requiredMode = Schema.RequiredMode.REQUIRED)
    private String finalSessionStatus;

    @NotNull(message = "Allow late join flag cannot be null.")
    @Schema(description = "Flag indicating if late joins were allowed.", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean allowLateJoin;

    @NotNull(message = "Power-ups enabled flag cannot be null.")
    @Schema(description = "Flag indicating if power-ups were enabled for the session.", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean powerUpsEnabled;

    @Schema(description = "Reason for session termination, if applicable.", example = "Host aborted game", nullable = true)
    private String terminationReason;

    @Schema(description = "Index of the slide where termination occurred, if applicable.", example = "2", nullable = true)
    private Integer terminationSlideIndex;

    @Valid
    @NotNull(message = "Players list cannot be null.")
    @Schema(description = "Array containing final results and details for each player.")
    private List<SessionPlayerDto> players;

    @Valid
    @NotNull(message = "Game slides list cannot be null.")
    @Schema(description = "Array containing information and all player answers for each slide presented during the session.")
    private List<SessionGameSlideDto> gameSlides;
}