package com.vuiquiz.quizwebsocket.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Don't include null fields (like details or validationErrors)
public class ErrorResponseDTO {

    private OffsetDateTime timestamp;
    private int status;
    private String error; // Short error description (e.g., "Not Found", "Bad Request")
    private String message; // More detailed error message
    private String path; // Request path where error occurred

    // Optional: For validation errors
    private Map<String, String> validationErrors;

    // Optional: For more detailed exception info (use carefully in production)
    private List<String> details;
}