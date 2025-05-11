// src/main/java/com/vuiquiz/quizwebsocket/dto/report/PlayerAnswerReportItemDto.java
package com.vuiquiz.quizwebsocket.dto.report;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vuiquiz.quizwebsocket.dto.ChoiceDTO; // Re-using
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Details of a single answer provided by a player, including question context.")
public class PlayerAnswerReportItemDto {

    @JsonProperty("answer")
    @Schema(description = "Details of the player's actual answer submission.")
    private AnswerDetails answerDetails;

    @JsonProperty("reportData")
    @Schema(description = "Contextual information about the question related to this answer.")
    private QuestionContextData questionContextData;


    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Specific details of the player's answer.")
    public static class AnswerDetails {
        @Schema(description = "Type of the answer record.", example = "quiz_answer")
        private String type; // e.g., "quiz_answer", "open_ended_answer"

        @Schema(description = "Index(es) of the choice selected by the player. Could be a single index, or array for jumble/multi-select. Stored as a string.", example = "2", nullable = true)
        private String choice; // From PlayerAnswer.choice (original string, might be JSON array)

        @Schema(description = "Text input by the player for open-ended questions.", nullable = true)
        private String text; // From PlayerAnswer.text

        @Schema(description = "Time taken by the player to submit this answer (milliseconds).", example = "7402")
        private Integer reactionTime; // From PlayerAnswer.reactionTimeMs

        @Schema(description = "Points awarded for this specific answer.", example = "0")
        private Integer points; // From PlayerAnswer.finalPoints

        // In Kahoot, gameId for answer includes timestamp. We'll use our session ID.
        // @Schema(description = "Identifier of the game session.", example = "311c7657-ef5a-40a2-8d06-af9b6b0b54f4")
        // private String gameSessionId; // Can be added if needed, but context implies it

        @Schema(description = "0-based index of the question/slide this answer pertains to.", example = "0")
        private Integer blockIndex; // GameSlide.slideIndex

        @Schema(description = "Type of the block/question.", example = "quiz")
        private String blockType; // GameSlide.slideType or from questionDistributionJson

        // cid and nickname are part of the parent context (player whose answers these are)
        // We can add them here if the structure strictly needs to match Kahoot 1:1

        @Schema(description = "Status of the answer (e.g., 'CORRECT', 'WRONG', 'TIMEOUT').", example = "WRONG")
        private String status; // From PlayerAnswer.status
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Contextual data of the question for which the answer was provided.")
    public static class QuestionContextData {
        @Schema(description = "The text displayed to the player for their chosen answer. For multiple choice, this is the text of the chosen option.", example = "Kiểm thử kiểm soát truy cập", nullable = true)
        private String displayText; // Derived: text of the chosen option(s)

        @Schema(description = "Title/text of the question.", example = "Đâu KHÔNG phải là một loại Kiểm thử chức năng?")
        private String blockTitle; // From GameSlide.questionDistributionJson.title

        @Schema(description = "0-based index of this question/slide in the game sequence.", example = "0")
        private Integer blockIndex; // GameSlide.slideIndex

        @Schema(description = "Original choices presented for this question.")
        private List<ChoiceDTO> blockChoices; // From GameSlide.questionDistributionJson.choices

        // We can omit blockLayout, blockMedia, gameIndex from Kahoot example for MVP
    }
}