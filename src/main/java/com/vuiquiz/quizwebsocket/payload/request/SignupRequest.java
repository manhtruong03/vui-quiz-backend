package com.vuiquiz.quizwebsocket.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request payload for user registration")
public class SignupRequest {
    @NotBlank
    @Size(min = 3, max = 50)
    @Schema(description = "Desired username for the new account", example = "newuser", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @Email
    @Size(max = 200)
    @Schema(description = "Email address for the new account", example = "newuser@example.com")
    private String email;

    @NotBlank
    @Size(min = 6, max = 30)
    @Schema(description = "Password for the new account", example = "securePassword123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
}