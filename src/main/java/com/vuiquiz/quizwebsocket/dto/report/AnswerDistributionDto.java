// src/main/java/com/vuiquiz/quizwebsocket/dto/report/AnswerDistributionDto.java
package com.vuiquiz.quizwebsocket.dto.report;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Represents the distribution of answers for a single choice of a question.")
public class AnswerDistributionDto {

    @Schema(description = "The text of the answer choice. Could be null if choice is image-based and text not provided.", example = "Kiểm thử tải trọng", nullable = true)
    private String answerText; // From the original question's choice

    @Schema(description = "The index of this choice in the original question's choice list.", example = "0")
    private Integer choiceIndex;

    @Schema(description = "The status if this choice was the correct one.", example = "CORRECT", nullable = true) // or "WRONG", or null if not applicable (e.g. for survey this could be different)
    private String status; // Indicates if THIS choice was correct in the question definition

    @Schema(description = "Number of players who selected this choice.", example = "14")
    private Integer count;

    // The Kahoot example has "answersSubmitted", "forCurrentUser", "isApproved", "isTextProfane"
    // For MVP, we'll focus on count and correctness of the choice itself.
}