// src/main/java/com/vuiquiz/quizwebsocket/model/PlayerAnswer.java
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
@Table(name = "player_answer", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"player_id", "slide_id"})
})
public class PlayerAnswer {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(name = "answer_id", updatable = false, nullable = false)
    private UUID answerId;

    @Column(name = "slide_id", nullable = false)
    private UUID slideId;

    @Column(name = "choice", length = 255) // Stores single choice as string or array of choices as JSON string
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
    @Builder.Default
    private Integer basePoints = 0;

    @Column(name = "final_points", nullable = false)
    @Builder.Default
    private Integer finalPoints = 0;

    @Column(name = "used_power_up_id")
    private UUID usedPowerUpId;

    @JdbcTypeCode(SqlTypes.JSON) // <<< --- ADD THIS ANNOTATION
    @Column(name = "used_power_up_context_json", columnDefinition = "jsonb")
    private String usedPowerUpContextJson;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @PrePersist
    protected void onPersist() {
        if (answerTimestamp == null) { // Set only if not already set (e.g., by DTO mapping)
            answerTimestamp = OffsetDateTime.now();
        }
    }
}