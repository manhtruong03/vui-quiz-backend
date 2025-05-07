package com.vuiquiz.quizwebsocket.payload.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {
    @NotBlank
    @Size(min = 3, max = 50)
    private String username;

    @Email
    @Size(max = 200)
    private String email; // Optional, adjust constraints if mandatory

    @NotBlank
    @Size(min = 6, max = 30) // Adjusted max length to match UserAccount model
    private String password;

    // You can add a field for role if you want users to specify it during signup
    // private String role;
}