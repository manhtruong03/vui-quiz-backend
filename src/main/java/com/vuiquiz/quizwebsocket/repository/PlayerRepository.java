// src/main/java/com/vuiquiz/quizwebsocket/repository/PlayerRepository.java
package com.vuiquiz.quizwebsocket.repository;

import com.vuiquiz.quizwebsocket.model.Player;
import org.springframework.data.domain.Page; // Import Page
import org.springframework.data.domain.Pageable; // Import Pageable
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface PlayerRepository extends JpaRepository<Player, UUID> {
    List<Player> findBySessionId(UUID sessionId);
    Page<Player> findBySessionId(UUID sessionId, Pageable pageable);
    Optional<Player> findBySessionIdAndNickname(UUID sessionId, String nickname);
    Optional<Player> findBySessionIdAndClientId(UUID sessionId, String clientId);
    List<Player> findBySessionIdAndStatus(UUID sessionId, String status);
    List<Player> findByUserId(UUID userId); // Players linked to a registered user account
}
