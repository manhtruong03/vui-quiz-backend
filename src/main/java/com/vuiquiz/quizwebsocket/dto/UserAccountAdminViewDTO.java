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
@Schema(description = "Data Transfer Object for viewing user account details by an administrator.")
public class UserAccountAdminViewDTO {

    @Schema(description = "Unique identifier of the user account.", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
    private UUID userId;

    @Schema(description = "Username of the user.", example = "admin_user")
    private String username;

    @Schema(description = "Email address of the user.", example = "admin@example.com")
    private String email;

    @Schema(description = "Role assigned to the user.", example = "ADMIN")
    private String role;

    @Schema(description = "Storage space used by the user in bytes.", example = "102400")
    private Long storageUsed;

    @Schema(description = "Maximum storage space allocated to the user in bytes.", example = "52428800")
    private Long storageLimit;

    @Schema(description = "Timestamp of when the user account was created.")
    private OffsetDateTime createdAt;

    @Schema(description = "Timestamp of the last update to the user account.")
    private OffsetDateTime updatedAt;

    @Schema(description = "Timestamp of when the user account was soft-deleted. Null if not deleted.", nullable = true)
    private OffsetDateTime deletedAt;
}