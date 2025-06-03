package com.vuiquiz.quizwebsocket.repository;

import com.vuiquiz.quizwebsocket.model.PlayerAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlayerAnswerRepository extends JpaRepository<PlayerAnswer, UUID> {
    Optional<PlayerAnswer> findByPlayerIdAndSlideId(UUID playerId, UUID slideId);
    List<PlayerAnswer> findByPlayerId(UUID playerId);
    List<PlayerAnswer> findBySlideId(UUID slideId);
    // To get all answers for a specific question in a specific game (across all players)
    // This would require joining GameSlide with originalQuestionId if not directly stored
    // List<PlayerAnswer> findByGameSlideOriginalQuestionId(UUID originalQuestionId); // Indirect
    List<PlayerAnswer> findByPlayerIdIn(List<UUID> playerIds);
    List<PlayerAnswer> findBySlideIdIn(List<UUID> slideIds);
}
