// src/main/java/com/vuiquiz/quizwebsocket/dto/report/SessionSummaryDto.java
package com.vuiquiz.quizwebsocket.dto.report;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Summary report for a completed game session.")
public class SessionSummaryDto {

    @Schema(description = "Type of the game session.", example = "LIVE")
    private String type; // From GameSession.gameType

    @Schema(description = "Name/Title of the quiz played.", example = "Các loại hình kiểm thử")
    private String name; // From Quiz.title

    @Schema(description = "Number of players who participated in the session.", example = "32")
    @JsonProperty("controllersCount")
    private Integer playerCount; // From GameSession.playerCount

    @Schema(description = "Total number of questions in the quiz.", example = "15")
    private Integer questionsCount; // From Quiz.questionCount or actual GameSlides of question type

    @Schema(description = "Overall average accuracy of answers across all players and scored questions.", example = "0.475")
    private Double averageAccuracy; // Calculated

    @Schema(description = "Unix timestamp (milliseconds) when the session started.", example = "1712106460139")
    private Long time; // From GameSession.startedAt

    @Schema(description = "Unix timestamp (milliseconds) when the session ended.", example = "1712107660525")
    private Long endTime; // From GameSession.endedAt

    @Schema(description = "Username of the host who conducted the session.", example = "tcongmanh2003")
    private String username; // From UserAccount.username (host)

    @Schema(description = "UUID of the host user.", example = "311c7657-ef5a-40a2-8d06-af9b6b0b54f4")
    private String hostId; // From GameSession.hostId

    @Schema(description = "Indicates if the quiz questions are scored.", example = "true")
    private Boolean isScored; // Inferred (e.g., if any question has points > 0)

    @Schema(description = "Indicates if questions have a defined correctness (not a survey).", example = "true")
    private Boolean hasCorrectness; // Inferred (e.g., if not all questions are survey type)

    @Schema(description = "Simplified information about the quiz played.")
    @JsonProperty("quizInfo")
    private QuizInfo quizInfo;

    @Schema(description = "Number of question slides for which answers were submitted.", example = "15")
    private Integer scoredBlocksWithAnswersCount; // Calculated: Question slides with at least one answer

    @Schema(description = "Overall average response time (milliseconds) for correct answers to scored questions.", example = "9926.98")
    private Double averageTime; // Calculated

    @Schema(description = "Average number of incorrect answers per player.", example = "5.875")
    private Double averageIncorrectAnswerCount; // Calculated

    // Simplified Quiz Info DTO
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class QuizInfo {
        @Schema(description = "UUID of the quiz.", example = "5c806f2d-a77f-4d7c-ad2c-d48589bb281d")
        private String quizId;
        @Schema(description = "Title of the quiz.", example = "Các loại hình kiểm thử")
        private String title;
        @Schema(description = "UUID of the quiz creator.", example = "311c7657-ef5a-40a2-8d06-af9b6b0b54f4")
        private String creatorUserId;
        @Schema(description = "Username of the quiz creator.", example = "tcongmanh2003")
        private String creatorUsername;
        // Add other relevant quiz fields if needed, e.g., visibility
    }
}