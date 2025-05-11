// src/main/java/com/vuiquiz/quizwebsocket/model/GameSlide.java
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
@Table(name = "game_slide", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"session_id", "slide_index"})
})
public class GameSlide {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(name = "slide_id", updatable = false, nullable = false)
    private UUID slideId;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "slide_index", nullable = false)
    private Integer slideIndex;

    @Column(name = "slide_type", nullable = false, length = 50)
    private String slideType;

    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "started_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime startedAt;

    @Column(name = "ended_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime endedAt;

    @JdbcTypeCode(SqlTypes.JSON) // <<< --- ADD THIS ANNOTATION
    @Column(name = "question_distribution_json", columnDefinition = "jsonb")
    private String questionDistributionJson;

    @Column(name = "original_question_id")
    private UUID originalQuestionId;
}