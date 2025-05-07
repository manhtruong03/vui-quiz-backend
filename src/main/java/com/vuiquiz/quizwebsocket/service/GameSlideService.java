package com.vuiquiz.quizwebsocket.service;

import com.vuiquiz.quizwebsocket.model.GameSlide;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameSlideService {
    GameSlide createGameSlide(GameSlide gameSlide); // Valid sessionId, originalQuestionId (if type is QUESTION)
    Optional<GameSlide> getGameSlideById(UUID slideId);
    List<GameSlide> getGameSlidesBySessionId(UUID sessionId); // Ordered by index
    Optional<GameSlide> getGameSlideBySessionIdAndIndex(UUID sessionId, Integer slideIndex);
    GameSlide updateGameSlideStatus(UUID slideId, String newStatus); // PENDING, ACTIVE, ENDED, SKIPPED
    // GameSlide advanceToNextSlide(UUID sessionId); // Complex logic for game flow
    void deleteGameSlidesBySessionId(UUID sessionId); // When a game session is deleted/aborted
}
