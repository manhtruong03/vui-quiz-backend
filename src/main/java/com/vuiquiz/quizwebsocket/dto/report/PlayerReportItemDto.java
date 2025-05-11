// src/main/java/com/vuiquiz/quizwebsocket/dto/report/PlayerReportItemDto.java
package com.vuiquiz.quizwebsocket.dto.report;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Report details for a single player in a game session.")
public class PlayerReportItemDto {

    @Schema(description = "Player's nickname.", example = "N1_VuTX")
    private String nickname;

    @Schema(description = "Player's rank in the session.", example = "1")
    private Integer rank;

    @Schema(description = "Total number of questions the player answered.", example = "15")
    @JsonProperty("answersCount")
    private Integer answerCount; // From Player.answerCount

    @Schema(description = "Number of questions the player did not answer (timeout/skipped).", example = "0")
    private Integer unansweredCount; // From Player.unansweredCount

    @Schema(description = "Number of questions the player answered correctly.", example = "11")
    @JsonProperty("correctAnswersCount")
    private Integer correctAnswers; // From Player.correctAnswers

    // Calculated in service
    @Schema(description = "Player's average accuracy (correct answers / total answered).", example = "0.7333")
    private Double averageAccuracy;

    @Schema(description = "Player's average points per answered question.", example = "709")
    private Double averagePoints; // Calculated: totalScore / answerCount

    @Schema(description = "Player's total accumulated points.", example = "10635")
    private Integer totalPoints; // From Player.totalScore

    @Schema(description = "Player's total reaction time across all answered questions (milliseconds).", example = "83907")
    private Long totalTime; // From Player.totalTime

    @Schema(description = "Player's average reaction time per answered question (milliseconds).", example = "5593.8")
    private Integer averageTime; // From Player.averageTime (already calculated on save)

    @Schema(description = "Player's longest answer streak.", example = "6")
    private Integer streakCount; // From Player.streakCount

    @Schema(description = "Internal Player UUID (primarily for backend use, can be exposed if useful).", example = "player-uuid-here")
    private String playerId; // Player.playerId

    @Schema(description = "Client ID used by the player during the session.", example = "websocket-client-id")
    private String clientId; // Player.clientId
}