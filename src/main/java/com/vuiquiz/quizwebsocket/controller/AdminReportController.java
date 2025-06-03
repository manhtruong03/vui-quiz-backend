package com.vuiquiz.quizwebsocket.controller;

import com.vuiquiz.quizwebsocket.dto.report.SessionSummaryDto;
import com.vuiquiz.quizwebsocket.payload.response.MessageResponse;
import com.vuiquiz.quizwebsocket.service.GameReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/reports")
@Tag(name = "Admin Game Reports API", description = "Endpoints for administrators to manage and view game session reports.")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class AdminReportController {

    private final GameReportService gameReportService;

    @GetMapping("/sessions")
    @Operation(summary = "List all game session reports (paginated)",
            description = "Retrieves a paginated list of all game session summaries. Default sort is by session end time, most recent first.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved list of session summaries.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))) // Page<SessionSummaryDto>
    @ApiResponse(responseCode = "401", description = "Unauthorized.")
    @ApiResponse(responseCode = "403", description = "Forbidden.")
    public ResponseEntity<Page<SessionSummaryDto>> getAllSessionReports(
            @PageableDefault(size = 10, sort = "endedAt", direction = Sort.Direction.DESC)
            @Parameter(description = "Pagination and sorting parameters. Sortable fields on GameSession: 'sessionId', 'gamePin', 'hostId', 'quizId', 'startedAt', 'endedAt', 'gameType', 'playerCount', 'status', 'createdAt'.")
            Pageable pageable) {
        log.info("Admin request to list all session reports with pageable: {}", pageable);
        Page<SessionSummaryDto> sessionSummaries = gameReportService.adminGetAllSessionSummaries(pageable);
        return ResponseEntity.ok(sessionSummaries);
    }

    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "Delete a game session report by ID",
            description = "Deletes a specific game session and all its associated data (players, slides, answers). This action also decrements the play count of the associated quiz.")
    @ApiResponse(responseCode = "200", description = "Game session report deleted successfully.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class)))
    @ApiResponse(responseCode = "204", description = "Game session report deleted successfully (alternative if no body).")
    @ApiResponse(responseCode = "401", description = "Unauthorized.")
    @ApiResponse(responseCode = "403", description = "Forbidden.")
    @ApiResponse(responseCode = "404", description = "Game session report not found.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class)))
    public ResponseEntity<MessageResponse> deleteGameSessionReport(
            @Parameter(description = "UUID of the game session report to delete.", required = true)
            @PathVariable UUID sessionId) {
        log.info("Admin request to delete session report with ID: {}", sessionId);
        gameReportService.adminDeleteGameSessionReport(sessionId); // ResourceNotFoundException handled by global handler or try-catch in controller
        return ResponseEntity.ok(new MessageResponse("Game session report with ID " + sessionId + " deleted successfully."));
        // Or return ResponseEntity.noContent().build();
    }
}