package com.vuiquiz.quizwebsocket.controller;

import com.vuiquiz.quizwebsocket.dto.TagAdminViewDTO;
import com.vuiquiz.quizwebsocket.dto.TagCreationRequestDTO;
import com.vuiquiz.quizwebsocket.dto.TagUpdateRequestDTO;
import com.vuiquiz.quizwebsocket.payload.response.MessageResponse;
import com.vuiquiz.quizwebsocket.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/tags")
@Tag(name = "Admin Tag Management", description = "APIs for administrators to manage tags.")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") // Apply security at class level
@SecurityRequirement(name = "bearerAuth") // Apply bearerAuth requirement for Swagger UI at class level
public class AdminTagController {

    private final TagService tagService;

    @PostMapping
    @Operation(summary = "Create a new tag", description = "Allows an administrator to create a new tag.")
    @ApiResponse(responseCode = "201", description = "Tag created successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TagAdminViewDTO.class)))
    @ApiResponse(responseCode = "400", description = "Invalid input data (e.g., validation error, tag name already exists)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token is missing or invalid.")
    @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role.")
    public ResponseEntity<?> createTag(@Valid @RequestBody TagCreationRequestDTO creationRequest) {
        try {
            TagAdminViewDTO createdTag = tagService.adminCreateTag(creationRequest);
            return new ResponseEntity<>(createdTag, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @GetMapping
    @Operation(summary = "List all tags (paginated)", description = "Retrieves a paginated list of all tags available in the system.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved list of tags.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))) // Schema for Page<TagAdminViewDTO>
    @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token is missing or invalid.")
    @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role.")
    public ResponseEntity<Page<TagAdminViewDTO>> getAllTags(
            @PageableDefault(size = 10, sort = "name")
            @Parameter(description = "Pagination and sorting parameters (e.g., page=0&size=10&sort=name,asc). Default sort is by name ascending.")
            Pageable pageable) {
        Page<TagAdminViewDTO> tags = tagService.adminGetAllTags(pageable);
        return ResponseEntity.ok(tags);
    }

    @GetMapping("/{tagId}")
    @Operation(summary = "Get tag details by ID", description = "Retrieves detailed information for a specific tag by its UUID.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved tag details.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TagAdminViewDTO.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token is missing or invalid.")
    @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role.")
    @ApiResponse(responseCode = "404", description = "Tag not found with the given UUID.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class)))
    public ResponseEntity<TagAdminViewDTO> getTagById(
            @Parameter(description = "UUID of the tag to retrieve.", required = true) @PathVariable UUID tagId) {
        // ResourceNotFoundException will be handled by a global exception handler or Spring's default
        TagAdminViewDTO tag = tagService.adminGetTagById(tagId);
        return ResponseEntity.ok(tag);
    }

    @PutMapping("/{tagId}")
    @Operation(summary = "Update tag details by ID", description = "Allows an administrator to update the name and/or description of an existing tag.")
    @ApiResponse(responseCode = "200", description = "Tag updated successfully.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TagAdminViewDTO.class)))
    @ApiResponse(responseCode = "400", description = "Invalid input data (e.g., validation error, new tag name already exists).",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token is missing or invalid.")
    @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role.")
    @ApiResponse(responseCode = "404", description = "Tag not found with the given UUID.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class)))
    public ResponseEntity<?> updateTag(
            @Parameter(description = "UUID of the tag to update.", required = true) @PathVariable UUID tagId,
            @Valid @RequestBody TagUpdateRequestDTO updateRequest) {
        try {
            TagAdminViewDTO updatedTag = tagService.adminUpdateTag(tagId, updateRequest);
            return ResponseEntity.ok(updatedTag);
        } catch (IllegalArgumentException e) { // Catching potential "name already exists"
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
        // ResourceNotFoundException will be handled globally
    }

    @DeleteMapping("/{tagId}")
    @Operation(summary = "Delete a tag by ID", description = "Soft-deletes a tag. The tag will no longer be visible or usable but remains in the database.")
    @ApiResponse(responseCode = "200", description = "Tag deleted successfully.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class)))
    @ApiResponse(responseCode = "204", description = "Tag deleted successfully (alternative response if no content body).")
    @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token is missing or invalid.")
    @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role.")
    @ApiResponse(responseCode = "404", description = "Tag not found with the given UUID.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class)))
    public ResponseEntity<MessageResponse> deleteTag(
            @Parameter(description = "UUID of the tag to delete.", required = true) @PathVariable UUID tagId) {
        tagService.deleteTag(tagId); // ResourceNotFoundException handled by global handler if not found
        return ResponseEntity.ok(new MessageResponse("Tag with ID " + tagId + " deleted successfully."));
        // Alternatively, return ResponseEntity.noContent().build();
    }
}