package com.vuiquiz.quizwebsocket.repository;

import com.vuiquiz.quizwebsocket.model.QuizTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuizTagRepository extends JpaRepository<QuizTag, UUID> {
    List<QuizTag> findByQuizId(UUID quizId);
    List<QuizTag> findByTagId(UUID tagId);
    Optional<QuizTag> findByQuizIdAndTagId(UUID quizId, UUID tagId);
    void deleteByQuizIdAndTagId(UUID quizId, UUID tagId);

    // Add this for efficient batch fetching
    List<QuizTag> findByQuizIdIn(List<UUID> quizIds);

    // Add this for deleting tags when a quiz is deleted
    void deleteByQuizId(UUID quizId);
}