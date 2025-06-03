package com.vuiquiz.quizwebsocket.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "DTO for viewing Tag details by an administrator.")
public class TagAdminViewDTO {

    @Schema(description = "Unique identifier of the tag.")
    private UUID tagId;

    @Schema(description = "Name of the tag.", example = "History")
    private String name;

    @Schema(description = "Description of the tag.", example = "Quizzes related to historical events.")
    private String description;

    @Schema(description = "Timestamp of when the tag was created.")
    private OffsetDateTime createdAt;

    @Schema(description = "Timestamp of the last update to the tag.")
    private OffsetDateTime updatedAt;

    // quizCount will be added in a later phase
    // @Schema(description = "Number of quizzes associated with this tag.")
    // private Integer quizCount;
}