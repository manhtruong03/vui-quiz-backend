package com.vuiquiz.quizwebsocket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Request DTO for an administrator to set a new password for a user account.")
public class AdminSetPasswordRequestDTO {

    @NotBlank(message = "New password cannot be blank.")
    @Size(min = 6, max = 30, message = "New password must be between 6 and 30 characters.")
    @Schema(description = "The new password for the user account.", example = "newSecurePassword123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newPassword;
}