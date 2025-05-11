// src/main/java/com/vuiquiz/quizwebsocket/service/GameReportService.java
package com.vuiquiz.quizwebsocket.service;

import com.vuiquiz.quizwebsocket.dto.report.PlayerAnswerReportItemDto; // Added
import com.vuiquiz.quizwebsocket.dto.report.PlayerReportItemDto;
import com.vuiquiz.quizwebsocket.dto.report.QuestionReportItemDto;
import com.vuiquiz.quizwebsocket.dto.report.SessionSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

public interface GameReportService {
    SessionSummaryDto getSessionSummary(UUID sessionId);
    Page<PlayerReportItemDto> getPlayerReports(UUID sessionId, Pageable pageable);
    Page<QuestionReportItemDto> getQuestionReports(UUID sessionId, Pageable pageable);
    Page<PlayerAnswerReportItemDto> getPlayerAnswersReport(UUID sessionId, UUID playerId, Pageable pageable); // New method
}