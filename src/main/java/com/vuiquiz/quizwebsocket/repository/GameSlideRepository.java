package com.vuiquiz.quizwebsocket.repository;

import com.vuiquiz.quizwebsocket.model.GameSlide;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GameSlideRepository extends JpaRepository<GameSlide, UUID> {
    List<GameSlide> findBySessionIdOrderBySlideIndexAsc(UUID sessionId);
    Optional<GameSlide> findBySessionIdAndSlideIndex(UUID sessionId, Integer slideIndex);
    List<GameSlide> findByOriginalQuestionId(UUID originalQuestionId);
}
