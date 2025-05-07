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
@Table(name = "player_answer", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"player_id", "slide_id"})
})
public class PlayerAnswer { // No soft delete in the schema

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(name = "answer_id", updatable = false, nullable = false)
    private UUID answerId;

    @Column(name = "slide_id", nullable = false) // Foreign key stored as UUID
    private UUID slideId;

    @Column(name = "choice", length = 255)
    private String choice;

    @Column(name = "text", columnDefinition = "TEXT")
    private String text;

    @Column(name = "reaction_time_ms", nullable = false)
    private Integer reactionTimeMs;

    @Column(name = "answer_timestamp", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime answerTimestamp;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "base_points", nullable = false)
    private Integer basePoints = 0;

    @Column(name = "final_points", nullable = false)
    private Integer finalPoints = 0;

    @Column(name = "used_power_up_id") // Foreign key stored as UUID
    private UUID usedPowerUpId;

    @Column(name = "used_power_up_context_json", columnDefinition = "jsonb")
    private String usedPowerUpContextJson; // JSONB as String

    @Column(name = "player_id", nullable = false) // Foreign key stored as UUID
    private UUID playerId;

    @PrePersist
    protected void onPersist() {
        if (answerTimestamp == null) {
            answerTimestamp = OffsetDateTime.now();
        }
    }
}