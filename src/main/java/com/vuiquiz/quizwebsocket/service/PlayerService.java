package com.vuiquiz.quizwebsocket.service;


import com.vuiquiz.quizwebsocket.model.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerService {
    Player createPlayer(Player player); // Requires valid sessionId, avatarId (optional), userId (optional)
    Optional<Player> getPlayerById(UUID playerId);
    List<Player> getPlayersBySessionId(UUID sessionId);
    Optional<Player> getPlayerBySessionIdAndNickname(UUID sessionId, String nickname);
    Player updatePlayerStatus(UUID playerId, String newStatus);
    Player updatePlayerScore(UUID playerId, int scoreToAdd, int correctAnswer, int streak); // Example for updating score
    void deletePlayer(UUID playerId); // Or kickPlayer(playerId)
    // Method to link player to UserAccount if they log in mid-game
}
