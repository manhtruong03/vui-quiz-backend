package com.vuiquiz.quizwebsocket.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
@Schema(description = "Request DTO for an administrator to create a new user account.")
public class UserAccountCreationRequestDTO {

    @NotBlank(message = "Username is required.")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters.")
    @Schema(description = "Desired username for the new account.", example = "new_teacher", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @Email(message = "Email should be valid.")
    @Size(max = 200, message = "Email must be at most 200 characters.")
    @Schema(description = "Email address for the new account. Optional.", example = "new.teacher@example.com")
    private String email; // Optional, but validated if present

    @NotBlank(message = "Password is required.")
    @Size(min = 6, max = 30, message = "Password must be between 6 and 30 characters.")
    @Schema(description = "Password for the new account.", example = "password123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @NotBlank(message = "Role is required.")
    @Schema(description = "Role to assign to the new user (e.g., 'TEACHER', 'ADMIN').", example = "TEACHER", requiredMode = Schema.RequiredMode.REQUIRED)
    private String role;
}