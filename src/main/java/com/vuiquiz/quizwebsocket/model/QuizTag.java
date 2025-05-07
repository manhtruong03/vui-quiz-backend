package com.vuiquiz.quizwebsocket.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "quiz_tag", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"quiz_id", "tag_id"})
})
public class QuizTag {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(name = "quiz_tag_id", updatable = false, nullable = false)
    private UUID quizTagId;

    @Column(name = "quiz_id", nullable = false) // Foreign key stored as UUID
    private UUID quizId;

    @Column(name = "tag_id", nullable = false) // Foreign key stored as UUID
    private UUID tagId;
}