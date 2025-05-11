// src/main/java/com/vuiquiz/quizwebsocket/model/Player.java
package com.vuiquiz.quizwebsocket.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode; // Import this
import org.hibernate.type.SqlTypes;          // Import this

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "player")
public class Player {

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
    @Builder.Default
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
    @Builder.Default
    private Integer totalScore = 0;

    @Column(name = "correct_answers", nullable = false)
    @Builder.Default
    private Integer correctAnswers = 0;

    @Column(name = "streak_count", nullable = false)
    @Builder.Default
    private Integer streakCount = 0;

    @Column(name = "answer_count", nullable = false)
    @Builder.Default
    private Integer answerCount = 0;

    @Column(name = "unanswered_count", nullable = false)
    @Builder.Default
    private Integer unansweredCount = 0;

    @Column(name = "avatar_id")
    private UUID avatarId;

    @Column(name = "total_time", nullable = false)
    @Builder.Default
    private Long totalTime = 0L;

    @Column(name = "average_time")
    private Integer averageTime;

    @JdbcTypeCode(SqlTypes.JSON) // <<< --- ADD THIS ANNOTATION
    @Column(name = "device_info_json", columnDefinition = "jsonb")
    private String deviceInfoJson;

    @Column(name = "last_activity_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime lastActivityAt;

    @Column(name = "client_id", nullable = false, length = 255)
    private String clientId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @PrePersist
    protected void onPersist() {
        if (joinedAt == null) { // Set only if not already set (e.g., by DTO mapping)
            joinedAt = OffsetDateTime.now();
        }
        if (lastActivityAt == null) { // Set only if not already set
            lastActivityAt = OffsetDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastActivityAt = OffsetDateTime.now();
    }
}