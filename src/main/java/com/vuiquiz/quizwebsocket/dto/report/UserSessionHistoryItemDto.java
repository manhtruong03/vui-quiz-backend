// src/main/java/com/vuiquiz/quizwebsocket/dto/report/UserSessionHistoryItemDto.java
package com.vuiquiz.quizwebsocket.dto.report;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Summary of a game session in a user's history (either hosted or participated).")
public class UserSessionHistoryItemDto {

    @Schema(description = "UUID of the game session.", example = "session-uuid-actual-value")
    private String sessionId;

    @Schema(description = "Name/Title of the quiz played in this session.", example = "Các loại hình kiểm thử")
    private String name;

    @Schema(description = "Unix timestamp (milliseconds) when the session started.", example = "1712106460139")
    private Long time;

    @Schema(description = "Unix timestamp (milliseconds) when the session ended.", example = "1712107660525", nullable = true)
    private Long endTime;

    @Schema(description = "Type of the game session.", example = "LIVE")
    private String type;

    @Schema(description = "Number of players who participated in this session.", example = "32")
    private Integer playerCount;

    @Schema(description = "The role of the current authenticated user in this specific session.", example = "HOST")
    private String roleInSession;

    @Schema(description = "UUID of the user who hosted this specific session.", example = "host-user-uuid")
    private String sessionHostUserId;

    @Schema(description = "Username of the user who hosted this specific session.", example = "tcongmanh2003")
    private String sessionHostUsername;

    @Schema(description = "UUID of the quiz used in this session.", example = "quiz-uuid-actual-value")
    private String quizId;
}