// src/main/java/com/vuiquiz/quizwebsocket/controller/GameResultController.java
package com.vuiquiz.quizwebsocket.controller;

import com.vuiquiz.quizwebsocket.dto.SessionFinalizationDto;
import com.vuiquiz.quizwebsocket.payload.response.MessageResponse;
import com.vuiquiz.quizwebsocket.service.GameResultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/session")
@Tag(name = "Game Result API", description = "Endpoints for managing game session results")
@RequiredArgsConstructor
@Slf4j
public class GameResultController {

    private final GameResultService gameResultService;

    @PostMapping("/finalize")
    @PreAuthorize("isAuthenticated()") // Or a more specific role/permission like "hasRole('HOST')" or "hasAuthority('FINALIZE_SESSION')"
    @Operation(summary = "Finalize a game session and save results",
            description = "Receives the complete game session data after it has ended and stores it in the database. This includes session details, player scores, slide information, and individual answers.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "201", description = "Game session results saved successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data")
    @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated or not authorized")
    @ApiResponse(responseCode = "500", description = "Internal server error during processing")
    public ResponseEntity<?> finalizeSession(@Valid @RequestBody SessionFinalizationDto sessionFinalizationDto) {
        try {
            log.info("Received request to finalize session for gamePin: {}", sessionFinalizationDto.getGamePin());
            String sessionId = gameResultService.saveSessionFinalization(sessionFinalizationDto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new MessageResponse("Game session results saved successfully. Session ID: " + sessionId));
        } catch (IllegalArgumentException e) {
            log.warn("Bad request while finalizing session for gamePin {}: {}", sessionFinalizationDto.getGamePin(), e.getMessage());
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error finalizing session for gamePin {}: {}", sessionFinalizationDto.getGamePin(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("An unexpected error occurred while saving session results."));
        }
    }
}