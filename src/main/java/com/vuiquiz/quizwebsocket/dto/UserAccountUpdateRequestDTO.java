package com.vuiquiz.quizwebsocket.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Request DTO for an administrator to update an existing user account. All fields are optional; only provided fields will be considered for update.")
public class UserAccountUpdateRequestDTO {

    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters if provided.")
    @Schema(description = "New username for the account. If null, username will not be changed.", example = "updated_user", nullable = true)
    private String username;

    @Email(message = "Email should be valid if provided.")
    @Size(max = 200, message = "Email must be at most 200 characters if provided.")
    @Schema(description = "New email address for the account. If null, email will not be changed.", example = "updated.user@example.com", nullable = true)
    private String email;

    @Schema(description = "New role for the account (e.g., 'TEACHER', 'ADMIN'). If null, role will not be changed.", example = "ADMIN", nullable = true)
    private String role;

    @PositiveOrZero(message = "Storage limit must be zero or positive if provided.")
    @Schema(description = "New storage limit for the user in bytes. If null, storage limit will not be changed.", example = "104857600", nullable = true)
    private Long storageLimit;
}