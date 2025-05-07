package com.vuiquiz.quizwebsocket.repository;

import com.vuiquiz.quizwebsocket.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlayerRepository extends JpaRepository<Player, UUID> {
    List<Player> findBySessionId(UUID sessionId);
    Optional<Player> findBySessionIdAndNickname(UUID sessionId, String nickname);
    Optional<Player> findBySessionIdAndClientId(UUID sessionId, String clientId);
    List<Player> findBySessionIdAndStatus(UUID sessionId, String status);
    List<Player> findByUserId(UUID userId); // Players linked to a registered user account
}
