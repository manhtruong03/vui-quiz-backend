// src/main/java/com/vuiquiz/quizwebsocket/service/GameReportService.java
package com.vuiquiz.quizwebsocket.service;

import com.vuiquiz.quizwebsocket.dto.report.*; // Ensure all report DTOs are imported
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

public interface GameReportService {
    SessionSummaryDto getSessionSummary(UUID sessionId);
    Page<PlayerReportItemDto> getPlayerReports(UUID sessionId, Pageable pageable);
    Page<QuestionReportItemDto> getQuestionReports(UUID sessionId, Pageable pageable);
    Page<PlayerAnswerReportItemDto> getPlayerAnswersReport(UUID sessionId, UUID playerId, Pageable pageable);
    // Method signature changed: no longer takes userId
    Page<UserSessionHistoryItemDto> getCurrentUserSessions(Pageable pageable);
}