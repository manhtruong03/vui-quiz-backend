package com.vuiquiz.quizwebsocket.service;


import com.vuiquiz.quizwebsocket.model.PlayerAnswer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerAnswerService {
    PlayerAnswer recordPlayerAnswer(PlayerAnswer playerAnswer); // Valid playerId, slideId, usedPowerUpId (optional)
    Optional<PlayerAnswer> getPlayerAnswerById(UUID answerId);
    Optional<PlayerAnswer> getAnswerByPlayerAndSlide(UUID playerId, UUID slideId);
    List<PlayerAnswer> getAnswersByPlayerId(UUID playerId);
    List<PlayerAnswer> getAnswersBySlideId(UUID slideId); // Useful for result aggregation
    // Calculation of points would happen here or before calling recordPlayerAnswer
}
