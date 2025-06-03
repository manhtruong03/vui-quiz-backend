// src/main/java/com/vuiquiz/quizwebsocket/dto/report/QuestionReportItemDto.java
package com.vuiquiz.quizwebsocket.dto.report;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vuiquiz.quizwebsocket.dto.ChoiceDTO; // Re-using existing ChoiceDTO for original choices
import com.vuiquiz.quizwebsocket.dto.VideoDetailDTO; // Re-using
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Report details and statistics for a single question (block) within a game session.")
public class QuestionReportItemDto {

    // --- Block Information (from GameSlide and its questionDistributionJson) ---
    @Schema(description = "0-based index of this question/slide in the game sequence.", example = "0")
    private Integer slideIndex; // from GameSlide.slideIndex

    @Schema(description = "Title or text of the question.", example = "Đâu KHÔNG phải là một loại Kiểm thử chức năng?")
    private String title; // from GameSlide.questionDistributionJson.title

    @Schema(description = "Type of the question (e.g., 'quiz', 'jumble').", example = "quiz")
    private String type; // from GameSlide.questionDistributionJson.type or GameSlide.slideType

    @Schema(description = "Description of the question/slide, if available (e.g., for content slides).", nullable = true)
    private String description;

    @Schema(description = "Time limit for the question in milliseconds. Null if not applicable.", example = "30000", nullable = true)
    private Integer time;

    @Schema(description = "Points multiplier for the question. Null if not applicable.", example = "1", nullable = true)
    private Integer pointsMultiplier;

    @Schema(description = "Original choices presented to the player for this question.")
    private List<ChoiceDTO> choices; // from GameSlide.questionDistributionJson.choices

    @Schema(description = "Media URL if any was associated with the question.", nullable = true)
    private String imageUrl; // from GameSlide.questionDistributionJson.image

    @Schema(description = "Video details if any was associated with the question.", nullable = true)
    private VideoDetailDTO video; // from GameSlide.questionDistributionJson.video

    @Schema(description = "Array of additional media elements (e.g., URLs or structured objects).", nullable = true)
    private List<String> media;

    // --- Report Data (Calculated from PlayerAnswers) ---
    @Schema(description = "Total number of answers submitted for this question.", example = "29")
    private Integer totalAnswers; // Count of PlayerAnswers for this slide

    @Schema(description = "Number of distinct players who answered this question.", example = "29")
    private Integer totalAnsweredControllers;

    @Schema(description = "Average accuracy for this question (correct answers / total valid answers). Null if not a gradable question.", example = "0.4375", nullable = true)
    private Double averageAccuracy;

    @Schema(description = "Average time (ms) taken by players to answer this question. Null if not a gradable question or no valid answers.", example = "8153.375", nullable = true)
    private Double averageTime;

    @Schema(description = "Distribution of answers across the choices for this question.")
    @JsonProperty("answersDistribution")
    private List<AnswerDistributionDto> answersDistribution;
}