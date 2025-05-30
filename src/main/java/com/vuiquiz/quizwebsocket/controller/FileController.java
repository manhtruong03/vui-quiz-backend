package com.vuiquiz.quizwebsocket.controller;

import com.vuiquiz.quizwebsocket.model.ImageStorage;
import com.vuiquiz.quizwebsocket.payload.response.MessageResponse;
import com.vuiquiz.quizwebsocket.service.FileStorageService;
import com.vuiquiz.quizwebsocket.service.ImageStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
// import io.swagger.v3.oas.annotations.media.SchemaProperty; // Removed this import as it's not used
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@Tag(name = "File Management API", description = "Endpoints for uploading and serving files.")
@CrossOrigin(origins = "*", maxAge = 3600) // Consider adjusting for production environment
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);
    private final FileStorageService fileStorageService;
    private final ImageStorageService imageStorageService;

    @Autowired
    public FileController(FileStorageService fileStorageService, ImageStorageService imageStorageService) {
        this.fileStorageService = fileStorageService;
        this.imageStorageService = imageStorageService;
    }

    @GetMapping("/files/images/{filename:.+}")
    @Operation(summary = "Download an image file",
            description = "Provides public access to download a previously uploaded image file by its filename.")
    @ApiResponse(responseCode = "200", description = "File downloaded successfully",
            content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)) // Or specify more specific media types if known (e.g., image/jpeg, image/png)
    @ApiResponse(responseCode = "404", description = "File not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = MessageResponse.class)))
    public ResponseEntity<Resource> downloadImageFile(
            @Parameter(description = "Filename of the image to download.", required = true, example = "example.jpg")
            @PathVariable String filename, HttpServletRequest request) {
        Resource resource = fileStorageService.loadFileAsResource(filename);

        String contentType = null;
        try {
            // Try to get content type from the ServletContext using the resource's file path
            String detectedContentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
            if (detectedContentType != null) {
                contentType = detectedContentType;
            } else {
                // Fallback if ServletContext cannot determine it
                // You can add logic for self-detection based on file extension if needed
                contentType = "application/octet-stream"; // Default content type
                logger.info("Could not determine file type via ServletContext for: " + filename + ". Defaulting to " + contentType);
            }
        } catch (IOException ex) {
            logger.info("Could not access file to determine its type for: " + filename + ". Defaulting to application/octet-stream.");
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"") // "inline" for the browser to attempt to display, "attachment" to always download
                .body(resource);
    }

    // Temporary Test Upload Endpoint for Stage 1
    @PostMapping(value = "/api/upload-test-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a test image (Temporary - Stage 2)",
            description = "A temporary endpoint for testing single image uploads. This version also creates an ImageStorage record in the database. Accepts 'imageFile' as the multipart file part name. A mock 'creatorId' can be provided as a request parameter for testing.")
    @ApiResponse(responseCode = "200", description = "Image uploaded and record created successfully. Returns image details.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(example = "{\"imageId\": \"uuid-goes-here\", \"originalFileName\": \"cat.jpg\", \"storedFileName\": \"uuid.jpg\", \"publicUrl\": \"http://localhost:8080/files/images/uuid.jpg\", \"contentType\": \"image/jpeg\", \"size\": 12345}")))
    @ApiResponse(responseCode = "400", description = "Bad request (e.g., no file, invalid file type)",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = MessageResponse.class)))
    @ApiResponse(responseCode = "500", description = "Internal server error during file upload",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = MessageResponse.class)))
    public ResponseEntity<?> uploadTestImage(
            @RequestParam("imageFile") MultipartFile imageFile,
            @RequestParam(name = "creatorId", required = false) String creatorIdString) { // Accept creatorId for testing
        if (imageFile == null || imageFile.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Please select a file to upload."));
        }

        UUID creatorId;
        if (StringUtils.hasText(creatorIdString)) {
            try {
                creatorId = UUID.fromString(creatorIdString);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(new MessageResponse("Invalid creatorId format. Must be a UUID."));
            }
        } else {
            // For Stage 2 testing, if no creatorId is provided, use a mock one.
            // In Stage 3 (Quiz creation), this will come from the authenticated user.
            creatorId = UUID.fromString("00000000-0000-0000-0000-000000000000"); // A mock UUID
            logger.warn("No creatorId provided for test upload, using mock UUID: {}", creatorId);
        }

        try {
            // 1. Store the file physically
            String storedFileName = fileStorageService.storeFile(imageFile);

            // 2. Create a record in ImageStorage table
            ImageStorage imageRecord = imageStorageService.createImageRecord(imageFile, storedFileName, creatorId);

            // 3. Get the public URL
            String publicUrl = imageStorageService.getPublicUrl(imageRecord); // Or getPublicUrl(storedFileName)

            Map<String, Object> response = new HashMap<>();
            response.put("imageId", imageRecord.getImageId());
            response.put("originalFileName", imageRecord.getFileName());
            response.put("storedFileName", imageRecord.getFilePath());
            response.put("publicUrl", publicUrl);
            response.put("contentType", imageRecord.getContentType());
            response.put("size", imageRecord.getFileSize());
            response.put("creatorId", imageRecord.getCreatorId()); // Echo back the creatorId used

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("An unexpected error occurred while uploading the file: " + imageFile.getOriginalFilename(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("An unexpected error occurred: " + e.getMessage()));
        }
    }
}