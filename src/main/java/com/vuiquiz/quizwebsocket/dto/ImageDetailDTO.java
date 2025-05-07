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
@Schema(description = "Details of an image, often used within question choices. The URL is the primary field.")
public class ImageDetailDTO {

    @Schema(description = "Direct URL (file_path) to the image. This is provided in requests and responses.",
            example = "https://placehold.co/200x200/blue/white?text=Square")
    private String url;

    @Schema(description = "Alternative text for the image", example = "A Square")
    private String altText;

    @Schema(description = "Content type of the image. Optional, backend might infer or retrieve if image is internal.",
            example = "image/png")
    private String contentType;
}