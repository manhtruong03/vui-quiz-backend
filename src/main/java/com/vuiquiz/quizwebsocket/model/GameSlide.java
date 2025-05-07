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
@Table(name = "game_slide", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"session_id", "slide_index"})
})
public class GameSlide { // No soft delete in the schema for this table

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(name = "slide_id", updatable = false, nullable = false)
    private UUID slideId;

    @Column(name = "session_id", nullable = false) // Foreign key stored as UUID
    private UUID sessionId;

    @Column(name = "slide_index", nullable = false)
    private Integer slideIndex;

    @Column(name = "slide_type", nullable = false, length = 50)
    private String slideType;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "PENDING";

    @Column(name = "started_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime startedAt;

    @Column(name = "ended_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime endedAt;

    @Column(name = "question_distribution_json", columnDefinition = "jsonb")
    private String questionDistributionJson; // JSONB as String

    @Column(name = "original_question_id") // Foreign key stored as UUID
    private UUID originalQuestionId;

    // Removed OneToMany relationships
}
