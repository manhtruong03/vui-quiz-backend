package com.vuiquiz.quizwebsocket.controller;

import com.vuiquiz.quizwebsocket.dto.AdminSetPasswordRequestDTO;
import com.vuiquiz.quizwebsocket.dto.UserAccountAdminViewDTO;
import com.vuiquiz.quizwebsocket.dto.UserAccountCreationRequestDTO;
import com.vuiquiz.quizwebsocket.dto.UserAccountUpdateRequestDTO;
import com.vuiquiz.quizwebsocket.exception.ResourceNotFoundException;
import com.vuiquiz.quizwebsocket.model.UserAccount;
import com.vuiquiz.quizwebsocket.payload.response.MessageResponse;
import com.vuiquiz.quizwebsocket.service.UserAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import java.util.UUID;
// Removed unused import: java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/users")
@Tag(name = "Admin User Account Management", description = "APIs for administrators to manage user accounts")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
@Slf4j
public class AdminUserAccountController {

    private final UserAccountService userAccountService;

    // Manual mapper method (can be replaced with MapStruct later if desired)
    private UserAccountAdminViewDTO toUserAccountAdminViewDTO(UserAccount userAccount) {
        if (userAccount == null) {
            return null;
        }
        return UserAccountAdminViewDTO.builder()
                .userId(userAccount.getUserId())
                .username(userAccount.getUsername())
                .email(userAccount.getEmail())
                .role(userAccount.getRole())
                .storageUsed(userAccount.getStorageUsed())
                .storageLimit(userAccount.getStorageLimit())
                .createdAt(userAccount.getCreatedAt())
                .updatedAt(userAccount.getUpdatedAt())
                .deletedAt(userAccount.getDeletedAt())
                .build();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all user accounts (paginated)",
            description = "Retrieves a paginated list of all user accounts. Requires ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Successfully retrieved list of user accounts",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Page.class))) // Swagger will show a generic Page here, Page<UserAccountAdminViewDTO> is implied
    @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token is missing or invalid")
    @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role")
    public ResponseEntity<Page<UserAccountAdminViewDTO>> getAllUsers(
            @PageableDefault(size = 10, sort = "username")
            @Parameter(description = "Pagination and sorting parameters (e.g., page=0&size=10&sort=username,asc)")
            Pageable pageable) {
        log.info("Admin request to list all users, pageable: {}", pageable);
        Page<UserAccount> userAccountsPage = userAccountService.getAllUsers(pageable);
        Page<UserAccountAdminViewDTO> userAccountAdminViewDTOPage = userAccountsPage
                .map(this::toUserAccountAdminViewDTO);
        return ResponseEntity.ok(userAccountAdminViewDTOPage);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user account by ID",
            description = "Retrieves a specific user account by its UUID. Requires ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Successfully retrieved user account details",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = UserAccountAdminViewDTO.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token is missing or invalid")
    @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role")
    @ApiResponse(responseCode = "404", description = "User account not found")
    public ResponseEntity<UserAccountAdminViewDTO> getUserById(
            @Parameter(description = "UUID of the user account to retrieve", required = true)
            @PathVariable UUID userId) {
        log.info("Admin request to get user by ID: {}", userId);
        UserAccount userAccount = userAccountService.getUserById(userId)
                .orElseThrow(() -> {
                    log.warn("User account with ID {} not found for admin request.", userId);
                    return new ResourceNotFoundException("UserAccount", "id", userId);
                });
        return ResponseEntity.ok(toUserAccountAdminViewDTO(userAccount));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new user account by an administrator",
            description = "Allows an administrator to create a new user account with specified details including username, password, email (optional), and role. Requires ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Details of the user account to create.",
            required = true,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = UserAccountCreationRequestDTO.class)))
    @ApiResponse(responseCode = "201", description = "User account created successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = UserAccountAdminViewDTO.class)))
    @ApiResponse(responseCode = "400", description = "Invalid input data (e.g., validation error, username/email already exists)",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = MessageResponse.class))) // Consistent with AuthController
    @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token is missing or invalid")
    @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role")
    public ResponseEntity<?> createUserAccount(@Valid @RequestBody UserAccountCreationRequestDTO creationRequest) {
        log.info("Admin request to create new user: {}", creationRequest.getUsername());
        try {
            UserAccountAdminViewDTO createdUser = userAccountService.createUserByAdmin(creationRequest);
            return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            // This exception is thrown by the service for uniqueness violations or role issues
            log.warn("Failed to create user by admin due to invalid argument: {}", e.getMessage());
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse(e.getMessage()));
        }
        // Other exceptions (like validation exceptions from @Valid) will be handled by a global exception handler if configured,
        // or result in a generic 500 error if not.
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an existing user account by an administrator",
            description = "Allows an administrator to update details of an existing user account such as username, email, role, and storage limit. " +
                    "Password updates should be handled by a separate dedicated endpoint. Requires ADMIN role. " +
                    "Only non-null fields in the request body will be considered for update.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @Parameter(name = "userId", description = "UUID of the user account to update", required = true, in = io.swagger.v3.oas.annotations.enums.ParameterIn.PATH)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "JSON object containing the user account fields to update. Fields not provided or null will not be changed.",
            required = true,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = UserAccountUpdateRequestDTO.class)))
    @ApiResponse(responseCode = "200", description = "User account updated successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = UserAccountAdminViewDTO.class)))
    @ApiResponse(responseCode = "400", description = "Invalid input data (e.g., validation error on DTO, username/email conflict)",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = MessageResponse.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token is missing or invalid")
    @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role")
    @ApiResponse(responseCode = "404", description = "User account not found with the given UUID",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = MessageResponse.class)))
    public ResponseEntity<?> updateUserAccount(
            @PathVariable UUID userId,
            @Valid @RequestBody UserAccountUpdateRequestDTO updateRequest) {
        log.info("Admin request to update user ID: {}", userId);
        try {
            UserAccountAdminViewDTO updatedUser = userAccountService.updateUserByAdmin(userId, updateRequest);
            return ResponseEntity.ok(updatedUser);
        } catch (ResourceNotFoundException e) {
            log.warn("User account with ID {} not found for update by admin. Message: {}", userId, e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse(e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("Failed to update user ID {} by admin due to invalid argument: {}", userId, e.getMessage());
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse(e.getMessage()));
        }
        // General validation errors from @Valid on UserAccountUpdateRequestDTO (if any constraints trigger)
        // would typically be handled by a @ControllerAdvice with @ExceptionHandler(MethodArgumentNotValidException.class)
        // to return a 400 with more detailed validation messages. If that's not in place, they might result in a 500.
    }

    @PostMapping("/{userId}/set-password")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Set or reset a user's password by an administrator",
            description = "Allows an administrator to set a new password for a specified user account. Requires ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @Parameter(name = "userId", description = "UUID of the user account whose password will be set/reset", required = true, in = io.swagger.v3.oas.annotations.enums.ParameterIn.PATH)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "JSON object containing the new password.", // Using alias from previous phases
            required = true,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AdminSetPasswordRequestDTO.class)))
    @ApiResponse(responseCode = "200", description = "Password updated successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = MessageResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid input data (e.g., password validation failed)",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = MessageResponse.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token is missing or invalid")
    @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role")
    @ApiResponse(responseCode = "404", description = "User account not found with the given UUID",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = MessageResponse.class)))
    public ResponseEntity<?> adminSetUserPassword(
            @PathVariable UUID userId,
            @Valid @RequestBody AdminSetPasswordRequestDTO setPasswordRequest) {
        log.info("Admin request to set password for user ID: {}", userId);
        try {
            userAccountService.adminSetUserPassword(userId, setPasswordRequest.getNewPassword());
            return ResponseEntity.ok(new MessageResponse("Password updated successfully for user " + userId));
        } catch (ResourceNotFoundException e) {
            log.warn("User account with ID {} not found for password set by admin. Message: {}", userId, e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse(e.getMessage()));
        }
        // Jakarta validation on AdminSetPasswordRequestDTO (e.g. @NotBlank, @Size)
        // will be handled by Spring and typically results in a 400 Bad Request
        // if a @ControllerAdvice with MethodArgumentNotValidException handler is present.
        // Otherwise, it might lead to a ConstraintViolationException from the service layer
        // if not caught by @Valid at controller level, or a generic 500.
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft delete a user account by an administrator",
            description = "Allows an administrator to soft-delete a user account. The account will be marked as deleted " +
                    "and will no longer be accessible through standard APIs or be able to log in. Requires ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @Parameter(name = "userId", description = "UUID of the user account to soft-delete", required = true, in = io.swagger.v3.oas.annotations.enums.ParameterIn.PATH)
    @ApiResponse(responseCode = "200", description = "User account soft-deleted successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = MessageResponse.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token is missing or invalid")
    @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role")
    @ApiResponse(responseCode = "404", description = "User account not found with the given UUID (or already deleted)",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = MessageResponse.class)))
    public ResponseEntity<?> deleteUserAccount(
            @PathVariable UUID userId) {
        log.info("Admin request to soft-delete user ID: {}", userId);
        try {
            userAccountService.deleteUserByAdmin(userId);
            return ResponseEntity.ok(new MessageResponse("User account " + userId + " soft-deleted successfully."));
        } catch (ResourceNotFoundException e) {
            log.warn("User account with ID {} not found for soft-deletion by admin. Message: {}", userId, e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse(e.getMessage()));
        }
    }
}