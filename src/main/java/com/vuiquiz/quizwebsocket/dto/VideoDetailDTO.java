package com.vuiquiz.quizwebsocket.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Details for a video (e.g., YouTube)")
public class VideoDetailDTO {

    @Schema(description = "Video ID from the service (e.g., YouTube video ID)", example = "dQw4w9WgXcQ")
    private String id;

    @Schema(description = "Start time for the video playback in seconds", example = "0.0")
    private Double startTime;

    @Schema(description = "End time for the video playback in seconds", example = "0.0")
    private Double endTime;

    @Schema(description = "Video service provider", example = "youtube")
    private String service;

    @Schema(description = "Full URL to the video (can be derived if ID and service are known)", example = "https://www.youtube.com/watch?v=dQw4w9WgXcQ")
    private String fullUrl;
}