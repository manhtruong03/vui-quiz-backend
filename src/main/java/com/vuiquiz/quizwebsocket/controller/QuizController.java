package com.vuiquiz.quizwebsocket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vuiquiz.quizwebsocket.dto.QuizDTO;
import com.vuiquiz.quizwebsocket.exception.FileStorageException;
import com.vuiquiz.quizwebsocket.exception.ResourceNotFoundException;
import com.vuiquiz.quizwebsocket.model.Quiz;
import com.vuiquiz.quizwebsocket.payload.response.MessageResponse;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/quizzes")
@Tag(name = "Quiz Management API", description = "Endpoints for creating, retrieving, and managing quizzes")
@Slf4j
public class QuizController {

    private final QuizService quizService;
    private final ObjectMapper objectMapper;
    private UUID creatorId;

    @Autowired
    public QuizController(QuizService quizService, ObjectMapper objectMapper) {
        this.quizService = quizService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE}) // Specify consumes
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create a new quiz with optional images",
            description = "Creates a new quiz including its questions. Allows uploading an optional cover image for the quiz and optional images for each question. " +
                    "The quiz data should be sent as a JSON string under the part name 'quizData'. " +
                    "The cover image file should be sent under the part name 'coverImageFile'. " +
                    "Question images should be sent as a list of files under the part name 'questionImageFiles', in the same order as the questions in 'quizData'.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "201", description = "Quiz created successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = QuizDTO.class)))
    @ApiResponse(responseCode = "400", description = "Invalid input data (e.g., JSON parsing error, validation error, file type error)",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = MessageResponse.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<?> createQuiz(
            @Parameter(description = "JSON string of QuizDTO", required = true) @Valid @RequestPart("quizData") String quizDataString,
            @Parameter(description = "Optional cover image file for the quiz") @RequestPart(name = "coverImageFile", required = false) MultipartFile coverImageFile,
            @Parameter(description = "Optional list of image files for questions, in order. Their count and order should correspond to questions in quizData.") @RequestPart(name = "questionImageFiles", required = false) List<MultipartFile> questionImageFiles) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        UUID creatorId = userDetails.getId();

        QuizDTO quizRequestDTO;
        try {
            quizRequestDTO = objectMapper.readValue(quizDataString, QuizDTO.class);
        } catch (Exception e) {
            log.error("Error parsing quizDataString for user {}: {}", creatorId, e.getMessage());
            return ResponseEntity.badRequest().body(new MessageResponse("Error parsing quizData JSON: " + e.getMessage()));
        }

        // Basic validation: if questionImageFiles are provided, their count should not exceed the number of questions.
        // More robust mapping (e.g. specific image for specific question index) can be done in the service.
        if (questionImageFiles != null && quizRequestDTO.getQuestions() != null && questionImageFiles.size() > quizRequestDTO.getQuestions().size()) {
            // This is a simplified check. Ideally, the client ensures the order and count.
            // Or, a more complex mapping strategy would be needed if files are not strictly ordered.
            log.warn("User {} provided {} question images for {} questions. Extra images will be ignored or this could be an error.",
                    creatorId, questionImageFiles.size(), quizRequestDTO.getQuestions().size());
            // Depending on strictness, you could return BadRequest here.
        }


        try {
            QuizDTO createdQuiz = quizService.createQuiz(quizRequestDTO, creatorId, coverImageFile, questionImageFiles);
            return new ResponseEntity<>(createdQuiz, HttpStatus.CREATED);
        } catch (FileStorageException e) {
            log.error("File storage error during quiz creation for user {}: {}", creatorId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse("File error: " + e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.error("Invalid argument during quiz creation for user {}: {}", creatorId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse("Invalid data: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during quiz creation for user {}: {}", creatorId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponse("An unexpected error occurred."));
        }
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

    @GetMapping("/my-quizzes")
    @PreAuthorize("isAuthenticated()") // User must be logged in
    @Operation(summary = "Get quizzes created by the current user",
            description = "Retrieves a paginated list of quizzes created by the currently authenticated user.")
    @ApiResponse(responseCode = "200", description = "List of user's quizzes retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized - User not logged in")
    @SecurityRequirement(name = "bearerAuth") // Indicates JWT is needed
    public ResponseEntity<Page<QuizDTO>> getCurrentUserQuizzes(
            @Parameter(description = "Pagination and sorting parameters (e.g., page=0&size=10&sort=modifiedAt,desc)")
            @PageableDefault(
                    size = 10,
                    sort = "modifiedAt", // Specify property name here
                    direction = Sort.Direction.DESC // Specify direction separately
            ) Pageable pageable) { // Default sort by modified date
        Page<QuizDTO> userQuizzes = quizService.getQuizzesByCurrentUser(pageable);
        return ResponseEntity.ok(userQuizzes);
    }

    @GetMapping("/public")
    @Operation(summary = "Get public and published quizzes",
            description = "Retrieves a paginated list of quizzes that are marked as public (visibility=1) and published (status='PUBLISHED').")
    @ApiResponse(responseCode = "200", description = "List of public quizzes retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class)))
    // Response is a Page
    public ResponseEntity<Page<QuizDTO>> getPublicPublishedQuizzes(
            @Parameter(description = "Pagination and sorting parameters (e.g., page=0&size=10&sort=createdAt,desc)")
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) { // Inject Pageable
        Page<QuizDTO> publicQuizzes = quizService.getPublicPublishedQuizzes(pageable);
        return ResponseEntity.ok(publicQuizzes);
    }

    @DeleteMapping("/{quizId}")
    @PreAuthorize("isAuthenticated()") // Or check if user is the owner
    @Operation(summary = "Delete a quiz by ID",
            description = "Deletes a quiz, its questions, associated tags, and all related images from storage. User storage quota will be updated.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "204", description = "Quiz deleted successfully")
    @ApiResponse(responseCode = "403", description = "Forbidden - User not authorized to delete this quiz")
    @ApiResponse(responseCode = "404", description = "Quiz not found")
    public ResponseEntity<Void> deleteQuiz(
            @Parameter(description = "ID of the quiz to delete", required = true) @PathVariable UUID quizId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        UUID currentUserId = userDetails.getId();

        // Optional: Check if the current user is the owner of the quiz before deleting
        Quiz quiz = quizService.getQuizById(quizId) // Assuming getQuizById returns Optional<Quiz>
                .orElseThrow(() -> new ResourceNotFoundException("Quiz", "id", quizId));
        if (!quiz.getCreatorId().equals(currentUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        quizService.deleteQuiz(quizId);
        return ResponseEntity.noContent().build();
    }
}