package com.vuiquiz.quizwebsocket.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import jakarta.persistence.*; // Ensure all are jakarta.persistence
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "quiz")
@SQLDelete(sql = "UPDATE quiz SET deleted_at = NOW() WHERE quiz_id = ?")
@Where(clause = "deleted_at IS NULL")
public class Quiz {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "quiz_id", updatable = false, nullable = false)
    private UUID quizId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "lobby_video_json", columnDefinition = "jsonb") // Stores serialized VideoDetailDTO
    private String lobbyVideoJson;

    @Column(name = "countdown_timer") // Default time for questions if not specified, or lobby timer. Made nullable.
    @Builder.Default
    private Integer countdownTimer = 5000;

    @Column(name = "question_count", nullable = false)
    @Builder.Default
    private Integer questionCount = 0; // Manually updated by service layer

    @Column(name = "play_count", nullable = false)
    @Builder.Default
    private Integer playCount = 0;

    @Column(name = "favorite_count", nullable = false)
    @Builder.Default
    private Integer favoriteCount = 0;

    @Column(name = "status", nullable = false, length = 50) // e.g., DRAFT, PUBLISHED
    @Builder.Default
    private String status = "PUBLISHED";

    @Column(name = "visibility", nullable = false) // e.g., 0 for private, 1 for public
    @Builder.Default
    private Integer visibility = 0;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt;

    @Column(name = "modified_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime modifiedAt;

    @Column(name = "deleted_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime deletedAt;

    @Column(name = "cover_image_id") // Foreign key to ImageStorage (UUID)
    private UUID coverImageId;

    @Column(name = "creator_id", nullable = false) // Foreign key to UserAccount (UUID)
    private UUID creatorId;

    // Optional: Field for 'quizType' from mock if it's distinct from 'status'
    @Column(name = "quiz_type_info", length = 50) // Example name to avoid conflict if 'type' is a keyword
    private String quizTypeInfo;


    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        modifiedAt = OffsetDateTime.now();
        if (this.status == null) this.status = "PUBLISHED";
        if (this.visibility == null) this.visibility = 0; // Default to private
        if (this.playCount == null) this.playCount = 0;
        if (this.favoriteCount == null) this.favoriteCount = 0;
        if (this.questionCount == null) this.questionCount = 0;
        // countdownTimer will use its @Builder.Default or be set explicitly
    }
}