package com.vuiquiz.quizwebsocket.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import jakarta.persistence.*; // Ensure all are jakarta.persistence
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "question_id", updatable = false, nullable = false)
    private UUID questionId;

    @Column(name = "quiz_id", nullable = false) // Manually managed foreign key to Quiz
    private UUID quizId;

    @Column(name = "question_type", nullable = false, length = 50) // "content", "quiz", "jumble", etc.
    private String questionType;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT") // For question.title or content.title
    private String questionText;

    @Column(name = "description_text", columnDefinition = "TEXT") // Specifically for content.description
    private String descriptionText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "answer_data_json", nullable = false, columnDefinition = "jsonb") // Stores serialized List<ChoiceDTO>
    private String answerDataJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "video_content_json", columnDefinition = "jsonb") // Stores serialized VideoDetailDTO
    private String videoContentJson;

    @Column(name = "points_multiplier") // Nullable, as 'content' and 'survey' might not have it
    private Integer pointsMultiplier;

    @Column(name = "time_limit") // Nullable, as 'content' might not have it. In milliseconds.
    private Integer timeLimit;

    @Column(name = "position", nullable = false) // 0-based order within the quiz
    private Integer position;

    @Column(name = "image_id") // Foreign key to ImageStorage (UUID) for the main question image
    private UUID imageId;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "fun_fact", columnDefinition = "TEXT")
    private String funFact;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime deletedAt;

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