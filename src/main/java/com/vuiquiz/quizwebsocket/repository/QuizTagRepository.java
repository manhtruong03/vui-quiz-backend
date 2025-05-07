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
}