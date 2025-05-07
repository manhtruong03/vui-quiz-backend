package com.vuiquiz.quizwebsocket.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "player"
        // The unique constraints from schema were commented out, if needed, add:
        // uniqueConstraints = {
        //     @UniqueConstraint(columnNames = {"session_id", "client_id"}),
        //     @UniqueConstraint(columnNames = {"session_id", "nickname"})
        // }
)
public class Player { // No soft delete in the schema for this table

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(name = "player_id", updatable = false, nullable = false)
    private UUID playerId;

    @Column(name = "nickname", nullable = false, length = 255)
    private String nickname;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "JOINING";

    @Column(name = "joined_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime joinedAt;

    @Column(name = "join_slide_index")
    private Integer joinSlideIndex;

    @Column(name = "waiting_since", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime waitingSince;

    @Column(name = "rank")
    private Integer rank;

    @Column(name = "total_score", nullable = false)
    private Integer totalScore = 0;

    @Column(name = "correct_answers", nullable = false)
    private Integer correctAnswers = 0;

    @Column(name = "streak_count", nullable = false)
    private Integer streakCount = 0;

    @Column(name = "answer_count", nullable = false)
    private Integer answerCount = 0;

    @Column(name = "unanswered_count", nullable = false)
    private Integer unansweredCount = 0;

    @Column(name = "avatar_id") // Foreign key stored as UUID
    private UUID avatarId;

    @Column(name = "total_time", nullable = false)
    private Long totalTime = 0L;

    @Column(name = "average_time")
    private Integer averageTime;

    @Column(name = "device_info_json", columnDefinition = "jsonb")
    private String deviceInfoJson; // JSONB as String

    @Column(name = "last_activity_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime lastActivityAt;

    @Column(name = "client_id", nullable = false, length = 255)
    private String clientId;

    @Column(name = "user_id") // Foreign key stored as UUID
    private UUID userId;

    @Column(name = "session_id", nullable = false) // Foreign key stored as UUID
    private UUID sessionId;

    // Removed OneToMany relationships

    @PrePersist
    protected void onPersist() {
        joinedAt = OffsetDateTime.now();
        lastActivityAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastActivityAt = OffsetDateTime.now();
    }
}
