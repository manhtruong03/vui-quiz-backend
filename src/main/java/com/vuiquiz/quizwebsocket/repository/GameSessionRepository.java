package com.vuiquiz.quizwebsocket.repository;

import com.vuiquiz.quizwebsocket.model.GameSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GameSessionRepository extends JpaRepository<GameSession, UUID> {
    Optional<GameSession> findByGamePin(String gamePin);
    List<GameSession> findByHostId(UUID hostId);
    List<GameSession> findByQuizId(UUID quizId);
    List<GameSession> findByStatus(String status);
    // Find active sessions (e.g., LOBBY or RUNNING) by gamePin
    Optional<GameSession> findByGamePinAndStatusIn(String gamePin, List<String> statuses);
}
