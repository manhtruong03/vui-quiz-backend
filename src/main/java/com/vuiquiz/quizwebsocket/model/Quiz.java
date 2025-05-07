package com.vuiquiz.quizwebsocket.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import jakarta.persistence.*;
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
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(name = "quiz_id", updatable = false, nullable = false)
    private UUID quizId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "lobby_video_json", columnDefinition = "jsonb")
    private String lobbyVideoJson; // JSONB as String

    @Column(name = "countdown_timer", nullable = false)
    private Integer countdownTimer = 5000;

    @Column(name = "question_count", nullable = false)
    private Integer questionCount = 0;

    @Column(name = "play_count", nullable = false)
    private Integer playCount = 0;

    @Column(name = "favorite_count", nullable = false)
    private Integer favoriteCount = 0;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "DRAFT";

    @Column(name = "visibility", nullable = false)
    private Integer visibility = 0;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt;

    @Column(name = "modified_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime modifiedAt;

    @Column(name = "deleted_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime deletedAt;

    @Column(name = "cover_image_id") // Foreign key stored as UUID
    private UUID coverImageId;

    @Column(name = "creator_id", nullable = false) // Foreign key stored as UUID
    private UUID creatorId;

    // Removed OneToMany relationships

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        modifiedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedAt = OffsetDateTime.now();
    }
}
