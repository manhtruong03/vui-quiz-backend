package com.vuiquiz.quizwebsocket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vuiquiz.quizwebsocket.dto.QuizDTO;
import com.vuiquiz.quizwebsocket.exception.*;
import com.vuiquiz.quizwebsocket.model.Quiz;
import com.vuiquiz.quizwebsocket.payload.response.MessageResponse;
import com.vuiquiz.quizwebsocket.security.services.UserDetailsImpl;
import com.vuiquiz.quizwebsocket.service.QuizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.StringToClassMapItem;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
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

    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Create a new quiz with explicitly keyed images",
            description = """
                Creates a new quiz by processing a multipart/form-data request. The request must include:
                1.  A **`quizData` part**: This part contains the quiz's metadata as a JSON string, conforming to the `QuizDTO` schema.
                    It is crucial that this part has a `Content-Type` header of `application/json`.
                    - The `QuizDTO` should specify `coverImageUploadKey` (string, optional) for the quiz's cover image.
                    - Each `QuestionDTO` within `quizData` can specify `questionImageUploadKey` (string, optional) for its associated image.
                2.  **Image file parts**: For each `coverImageUploadKey` or `questionImageUploadKey` provided in the `quizData` JSON,
                    a corresponding binary file part must be sent.
                    - The **name of each image file part MUST exactly match** the corresponding `uploadKey` string from `quizData`.
                    For instance, if `quizData` includes `"coverImageUploadKey": "promoImage123"`, then the multipart request must contain
                    a file part named `promoImage123` with the image data.
                """,
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(

                    description = "Multipart request containing quiz data and image files.",

                    required = true,

                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(
                                    type = "object",
                                    requiredProperties = {"quizData"} // Specifies that 'quizData' part is mandatory
                            ),
                            encoding = {
                                    @Encoding(
                                            name = "quizData", // Must match a property name in the schema above
                                            contentType = "application/json" // Ensures this part is treated as JSON
                                    )
                                    // Encoding for dynamic file parts (e.g., 'cover123', 'q_image_abc') cannot be explicitly listed here
                                    // as their names are not fixed. The client should set the appropriate Content-Type for each image file part
                                    // (e.g., image/jpeg, image/png).
                            },
                            examples = {
                                    @ExampleObject(
                                            name = "Quiz with Cover and Question Image",
                                            summary = "Example multipart request payload",
                                            description = """
                                                Illustrates sending `quizData` (as application/json) and two image files.
                                                The part names for images (`cover123`, `q_image_abc`) MUST match the keys specified in `quizData`.
                                                (Note: Binary data is represented conceptually below).
                                                """,
                                            value = """
                                                --boundary
                                                Content-Disposition: form-data; name="quizData"
                                                Content-Type: application/json

                                                {
                                                  "title": "My Keyed Image Quiz",
                                                  "coverImageUploadKey": "cover123",
                                                  "questions": [
                                                    {
                                                      "title": "Question 1 with Image",
                                                      "questionImageUploadKey": "q_image_abc"
                                                      /* other question props */
                                                    }
                                                  ]
                                                  /* other quiz props */
                                                }
                                                --boundary
                                                Content-Disposition: form-data; name="cover123"; filename="cover.jpg"
                                                Content-Type: image/jpeg

                                                (binary image data for cover.jpg)
                                                --boundary
                                                Content-Disposition: form-data; name="q_image_abc"; filename="question_pic.png"
                                                Content-Type: image/png

                                                (binary image data for question_pic.png)
                                                --boundary--
                                                """
                                    )
                            }
                    )
            )
    )
    @ApiResponse(responseCode = "201", description = "Quiz created successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = QuizDTO.class)))
    @ApiResponse(responseCode = "400", description = "Invalid input data (e.g., JSON parsing error, validation error, file type error)",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = MessageResponse.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<?> createQuiz(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        UUID creatorId = userDetails.getId();

        if (!(request instanceof MultipartHttpServletRequest multipartRequest)) {
            log.error("Request is not a multipart request for user {}", creatorId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse("Invalid request: Expected a multipart request."));
        }

        String quizDataString = null;
        Map<String, MultipartFile> imageFilesForService = new HashMap<>();

        try {
            // Attempt to get quizData part
            jakarta.servlet.http.Part quizDataPart = multipartRequest.getPart("quizData");
            if (quizDataPart != null) {
                // Log the content type of the quizData part for debugging
                log.info("quizData part received. Name: {}, ContentType: {}, Size: {}",
                        quizDataPart.getName(), quizDataPart.getContentType(), quizDataPart.getSize());

                // Recommended: Client should send quizData part with Content-Type: application/json
                // If it's not application/json, parsing might be based on default charset.
                if (quizDataPart.getContentType() != null &&
                        !quizDataPart.getContentType().toLowerCase().contains("application/json") &&
                        !quizDataPart.getContentType().toLowerCase().contains("text/plain")) {
                    log.warn("Content-Type of 'quizData' part is '{}'. Expected 'application/json' or 'text/plain'. Attempting to read as UTF-8 string.", quizDataPart.getContentType());
                }
                quizDataString = StreamUtils.copyToString(quizDataPart.getInputStream(), StandardCharsets.UTF_8);
            } else {
                // Fallback: try getParameter if it was sent as a simple form field (less likely for JSON)
                quizDataString = multipartRequest.getParameter("quizData");
                if (quizDataString != null) {
                    log.warn("'quizData' was retrieved as a request parameter. Ensure client sends it as a distinct multipart part, ideally with Content-Type 'application/json'.");
                }
            }

            if (quizDataString == null || quizDataString.trim().isEmpty()) {
                log.error("quizData part is missing or empty in the multipart request for user {}", creatorId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse("Missing or empty 'quizData' part in the request."));
            }

            // Populate the imageFiles map for the service
            // getFileMap() returns all parts that are files.
            Map<String, MultipartFile> allUploadedFiles = multipartRequest.getFileMap();
            for (Map.Entry<String, MultipartFile> entry : allUploadedFiles.entrySet()) {
                // We assume that if 'quizData' was sent as a file (e.g. client uploaded a .json file for it),
                // its key would be "quizData". All other file parts are actual images.
                if (!entry.getKey().equals("quizData")) {
                    if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                        imageFilesForService.put(entry.getKey(), entry.getValue());
                        log.info("Adding image file to service map: key='{}', originalFilename='{}', size={}",
                                entry.getKey(), entry.getValue().getOriginalFilename(), entry.getValue().getSize());
                    }
                } else {
                    // This case means quizData itself was uploaded as a file.
                    // We've already handled reading its content above if getPart("quizData") was used.
                    // If quizDataString was populated via getParameter, this condition won't be met for "quizData".
                    log.info("Part named 'quizData' was also found in the file map. Content already read if it was a file part.");
                }
            }
            if(imageFilesForService.isEmpty() && !allUploadedFiles.isEmpty() && !allUploadedFiles.containsKey("quizData")){
                // This might happen if keys in allUploadedFiles are not matching uploadKeys but are not "quizData" either.
                // Or if all files are named 'quizData' which would be wrong.
                log.warn("imageFilesForService is empty, but allUploadedFiles map was not. Keys in allUploadedFiles: {}. This might indicate a mismatch in expected part names vs. sent part names for images.", allUploadedFiles.keySet());
            }


            log.debug("Parsed quizDataString (first 200 chars): {}", quizDataString.substring(0, Math.min(quizDataString.length(), 200)));
            log.info("Number of image files prepared for service: {}", imageFilesForService.size());
            imageFilesForService.forEach((key, file) -> log.debug("Service image file: {} -> {}", key, file.getOriginalFilename()));


            QuizDTO quizRequestDTO = objectMapper.readValue(quizDataString, QuizDTO.class);

            QuizDTO createdQuiz = quizService.createQuiz(quizRequestDTO, creatorId, imageFilesForService);
            return new ResponseEntity<>(createdQuiz, HttpStatus.CREATED);

        } catch (IOException | ServletException e) {
            log.error("Error processing multipart request for user {}: {}", creatorId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse("Error processing multipart request: " + e.getMessage()));
        } catch (StorageQuotaExceededException | FileStorageException e) {
            log.error("Storage or file error during quiz creation for user {}: {}", creatorId, e.getMessage());
            // Consider a more specific HTTP status if needed, e.g., HttpStatus.INSUFFICIENT_STORAGE for quota
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(new MessageResponse("File error: " + e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.error("Invalid argument during quiz creation for user {}: {}", creatorId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse("Invalid data: " + e.getMessage()));
        } catch (Exception e) { // Catch-all for other unexpected errors
            log.error("Unexpected error during quiz creation for user {}: {}", creatorId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponse("An unexpected error occurred: " + e.getMessage()));
        }
    }


    @GetMapping("/{quizId}")
    @Operation(summary = "Get full quiz details by ID",
            description = "Retrieves a specific quiz. Access is conditional: " +
                    "Public quizzes (visibility=1) require authentication. " +
                    "Private quizzes (visibility=0) require the requester to be the owner.")
    @ApiResponse(responseCode = "200", description = "Quiz details found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = QuizDTO.class)))
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(responseCode = "403", description = "Forbidden - Not authorized to view this quiz")
    @ApiResponse(responseCode = "404", description = "Quiz not found")
    public ResponseEntity<QuizDTO> getQuizDetailsById(
            @Parameter(description = "ID of the quiz to retrieve") @PathVariable UUID quizId) {

        QuizDTO quizDTO = quizService.getQuizDetailsById(quizId); // This already handles ResourceNotFoundException

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() &&
                !(authentication.getPrincipal() instanceof String && "anonymousUser".equals(authentication.getPrincipal()));

        if (quizDTO.getVisibility() == 0) { // Private quiz
            if (!isAuthenticated) {
                throw new UnauthorizedException("Authentication is required to access this private quiz.");
            }
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            UUID currentUserId = userDetails.getId();
            if (quizDTO.getCreatorId() == null || !quizDTO.getCreatorId().equals(currentUserId)) {
                throw new ForbiddenAccessException("You are not authorized to view this private quiz.");
            }
        } else if (quizDTO.getVisibility() == 1) { // Public quiz
            if (!isAuthenticated) {
                // As per your requirement: "if quiz public (visibility=1...) non-logged in users will require authentication."
                throw new UnauthorizedException("Authentication is required to view public quizzes.");
            }
            // Authenticated users can access public quizzes.
        } else {
            // Should not happen if visibility is strictly 0 or 1
            throw new IllegalStateException("Quiz visibility is not set correctly.");
        }

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