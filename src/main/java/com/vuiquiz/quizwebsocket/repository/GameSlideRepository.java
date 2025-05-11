// src/main/java/com/vuiquiz/quizwebsocket/repository/GameSlideRepository.java
package com.vuiquiz.quizwebsocket.repository;

import com.vuiquiz.quizwebsocket.model.GameSlide;
import org.springframework.data.domain.Page; // Import
import org.springframework.data.domain.Pageable; // Import
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

    // New method for fetching specific slide types with pagination
    Page<GameSlide> findBySessionIdAndSlideTypeIn(UUID sessionId, List<String> slideTypes, Pageable pageable); // <<<--- ADD THIS
}