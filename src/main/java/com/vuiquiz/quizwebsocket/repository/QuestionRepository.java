package com.vuiquiz.quizwebsocket.repository;

import com.vuiquiz.quizwebsocket.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuestionRepository extends JpaRepository<Question, UUID> {
    List<Question> findByQuizIdOrderByPositionAsc(UUID quizId);
    // If you need to count questions for a quiz efficiently
    long countByQuizId(UUID quizId);
    void deleteByQuizId(UUID quizId);
    List<Question> findByQuizIdIn(List<UUID> quizIds);
}
