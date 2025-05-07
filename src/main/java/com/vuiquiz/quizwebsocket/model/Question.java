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
@Table(name = "question")
@SQLDelete(sql = "UPDATE question SET deleted_at = NOW() WHERE question_id = ?")
@Where(clause = "deleted_at IS NULL")
public class Question {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(name = "question_id", updatable = false, nullable = false)
    private UUID questionId;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "quiz_id", nullable = false) // Foreign key stored as UUID
    private UUID quizId;

    @Column(name = "question_type", nullable = false, length = 50)
    private String questionType;

    @Column(name = "answer_data_json", nullable = false, columnDefinition = "jsonb")
    private String answerDataJson; // JSONB as String

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "fun_fact", columnDefinition = "TEXT")
    private String funFact;

    @Column(name = "video_content_json", columnDefinition = "jsonb")
    private String videoContentJson; // JSONB as String

    @Column(name = "points_multiplier", nullable = false)
    private Integer pointsMultiplier = 1;

    @Column(name = "time_limit", nullable = false)
    private Integer timeLimit = 20000; // ms

    @Column(name = "position", nullable = false)
    private Integer position;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime deletedAt;

    @Column(name = "image_id") // Foreign key stored as UUID
    private UUID imageId;

    // Removed OneToMany relationships

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
