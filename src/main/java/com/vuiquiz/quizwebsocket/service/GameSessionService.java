package com.vuiquiz.quizwebsocket.service;

import com.vuiquiz.quizwebsocket.model.GameSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameSessionService {
    GameSession createGameSession(GameSession gameSession); // Requires valid hostId, quizId
    Optional<GameSession> getGameSessionById(UUID sessionId);
    Optional<GameSession> getGameSessionByPin(String gamePin);
    Optional<GameSession> getActiveGameSessionByPin(String gamePin); // e.g. LOBBY or RUNNING
    List<GameSession> getGameSessionsByHostId(UUID hostId);
    List<GameSession> getGameSessionsByQuizId(UUID quizId);
    Page<GameSession> getAllGameSessions(Pageable pageable);
    GameSession updateGameSessionStatus(UUID sessionId, String newStatus);
    GameSession startGameSession(UUID sessionId);
    GameSession endGameSession(UUID sessionId, String terminationReason, Integer slideIndex);
    void deleteGameSession(UUID sessionId); // Usually sessions are ended, not deleted
}