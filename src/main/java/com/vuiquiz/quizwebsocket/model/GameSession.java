// src/main/java/com/vuiquiz/quizwebsocket/model/GameSession.java
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
@Table(name = "game_session") // Removed unique constraint on game_pin from @Table annotation
public class GameSession { // No soft delete in the schema for this table

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(name = "session_id", updatable = false, nullable = false)
    private UUID sessionId;

    @Column(name = "game_pin", nullable = false, length = 20) // unique = true REMOVED from @Column if it was there
    private String gamePin;

    @Column(name = "host_id", nullable = false) // Foreign key stored as UUID
    private UUID hostId;

    @Column(name = "quiz_id", nullable = false) // Foreign key stored as UUID
    private UUID quizId;

    @Column(name = "started_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime startedAt;

    @Column(name = "ended_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime endedAt;

    @Column(name = "game_type", nullable = false, length = 50)
    @Builder.Default
    private String gameType = "LIVE";

    @Column(name = "player_count", nullable = false)
    @Builder.Default
    private Integer playerCount = 0;

    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private String status = "LOBBY";

    @Column(name = "allow_late_join", nullable = false)
    @Builder.Default
    private boolean allowLateJoin = true;

    @Column(name = "power_ups_enabled", nullable = false)
    @Builder.Default
    private boolean powerUpsEnabled = true;

    @Column(name = "termination_reason", columnDefinition = "TEXT")
    private String terminationReason;

    @Column(name = "termination_slide_index")
    private Integer terminationSlideIndex;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}