package com.vuiquiz.quizwebsocket.controller;

import com.vuiquiz.quizwebsocket.dto.QuizDTO;
import com.vuiquiz.quizwebsocket.security.services.UserDetailsImpl;
import com.vuiquiz.quizwebsocket.service.QuizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/quizzes")
@Tag(name = "Quiz Management API", description = "Endpoints for creating, retrieving, and managing quizzes")
public class QuizController {

    private final QuizService quizService;

    @Autowired
    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create a new quiz",
            description = "Creates a new quiz including its questions. The response contains quiz metadata; questions are not re-fetched in this response.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "201", description = "Quiz created successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = QuizDTO.class)))
    // ... other ApiResponse annotations
    public ResponseEntity<QuizDTO> createQuiz(@Valid @RequestBody QuizDTO quizRequestDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        UUID creatorId = userDetails.getId();

        QuizDTO createdQuiz = quizService.createQuiz(quizRequestDTO, creatorId);
        return new ResponseEntity<>(createdQuiz, HttpStatus.CREATED);
    }

    @GetMapping("/{quizId}")
    @Operation(summary = "Get full quiz details by ID",
            description = "Retrieves a specific quiz along with all its questions and their details.")
    @ApiResponse(responseCode = "200", description = "Quiz details found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = QuizDTO.class)))
    @ApiResponse(responseCode = "404", description = "Quiz not found")
    public ResponseEntity<QuizDTO> getQuizDetailsById( // Renamed controller method for clarity too, though not strictly necessary
                                                       @Parameter(description = "ID of the quiz to retrieve") @PathVariable UUID quizId) {
        QuizDTO quizDTO = quizService.getQuizDetailsById(quizId); // Call the renamed service method
        return ResponseEntity.ok(quizDTO);
    }
}