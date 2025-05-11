// src/main/java/com/vuiquiz/quizwebsocket/controller/GameReportController.java
package com.vuiquiz.quizwebsocket.controller;

import com.vuiquiz.quizwebsocket.dto.report.PlayerAnswerReportItemDto; // Added
import com.vuiquiz.quizwebsocket.dto.report.PlayerReportItemDto;
import com.vuiquiz.quizwebsocket.dto.report.QuestionReportItemDto;
import com.vuiquiz.quizwebsocket.dto.report.SessionSummaryDto;
import com.vuiquiz.quizwebsocket.service.GameReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/reports")
@Tag(name = "Game Reports API", description = "Endpoints for retrieving game session reports and statistics.")
@RequiredArgsConstructor
public class GameReportController {

    private final GameReportService gameReportService;

    @GetMapping("/sessions/{sessionId}/summary")
    @Operation(summary = "Get a summary report for a specific game session.",
            description = "Provides an overview of a completed game session, including general statistics, quiz information, and host details.")
    @ApiResponse(responseCode = "200", description = "Session summary retrieved successfully.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = SessionSummaryDto.class)))
    @ApiResponse(responseCode = "404", description = "Game session or related data not found.")
    public ResponseEntity<SessionSummaryDto> getSessionSummary(
            @Parameter(description = "UUID of the game session to retrieve the summary for.", required = true)
            @PathVariable UUID sessionId) {
        SessionSummaryDto summary = gameReportService.getSessionSummary(sessionId);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/sessions/{sessionId}/players")
    @Operation(summary = "Get a paginated list of player reports for a specific game session.",
            description = "Retrieves player performance data for a given session, including rank, score, accuracy, and timing. Supports pagination and sorting.")
    @ApiResponse(responseCode = "200", description = "Player reports retrieved successfully.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class)))
    @ApiResponse(responseCode = "404", description = "Game session not found.")
    public ResponseEntity<Page<PlayerReportItemDto>> getPlayerReports(
            @Parameter(description = "UUID of the game session.", required = true)
            @PathVariable UUID sessionId,
            @PageableDefault(size = 10, sort = "rank")
            @Parameter(description = "Pagination and sorting parameters (e.g., page=0&size=10&sort=totalPoints,desc)")
            Pageable pageable) {
        Page<PlayerReportItemDto> playerReports = gameReportService.getPlayerReports(sessionId, pageable);
        return ResponseEntity.ok(playerReports);
    }

    @GetMapping("/sessions/{sessionId}/questions")
    @Operation(summary = "Get a paginated list of question reports for a specific game session.",
            description = "Retrieves question details and aggregated player performance for each question in a session. Supports pagination and sorting by slide index.")
    @ApiResponse(responseCode = "200", description = "Question reports retrieved successfully.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class)))
    @ApiResponse(responseCode = "404", description = "Game session not found.")
    public ResponseEntity<Page<QuestionReportItemDto>> getQuestionReports(
            @Parameter(description = "UUID of the game session.", required = true)
            @PathVariable UUID sessionId,
            @PageableDefault(size = 10, sort = "slideIndex", direction = Sort.Direction.ASC)
            @Parameter(description = "Pagination and sorting parameters (e.g., page=0&size=10&sort=slideIndex,asc)")
            Pageable pageable) {
        Page<QuestionReportItemDto> questionReports = gameReportService.getQuestionReports(sessionId, pageable);
        return ResponseEntity.ok(questionReports);
    }

    @GetMapping("/sessions/{sessionId}/players/{playerId}/answers")
    @Operation(summary = "Get a paginated list of a specific player's answers for a game session.",
            description = "Retrieves all answers submitted by a particular player in a session, along with the context of each question. Supports pagination and sorting by question index.")
    @ApiResponse(responseCode = "200", description = "Player's answers retrieved successfully.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class)))
    @ApiResponse(responseCode = "404", description = "Game session or player not found, or player does not belong to the session.")
    public ResponseEntity<Page<PlayerAnswerReportItemDto>> getPlayerAnswersReport(
            @Parameter(description = "UUID of the game session.", required = true)
            @PathVariable UUID sessionId,
            @Parameter(description = "UUID of the player.", required = true)
            @PathVariable UUID playerId,
            @PageableDefault(size = 10, sort = "blockIndex", direction = Sort.Direction.ASC) // Default sort by slide/block index
            @Parameter(description = "Pagination and sorting parameters (e.g., page=0&size=10&sort=blockIndex,asc)")
            Pageable pageable) {
        Page<PlayerAnswerReportItemDto> playerAnswersReport = gameReportService.getPlayerAnswersReport(sessionId, playerId, pageable);
        return ResponseEntity.ok(playerAnswersReport);
    }
}