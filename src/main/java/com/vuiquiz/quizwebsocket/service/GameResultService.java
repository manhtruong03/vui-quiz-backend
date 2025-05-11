// src/main/java/com/vuiquiz/quizwebsocket/service/GameResultService.java
package com.vuiquiz.quizwebsocket.service;

import com.vuiquiz.quizwebsocket.dto.SessionFinalizationDto; // Assuming this DTO will be created based on session-finalization.dto.ts

public interface GameResultService {
    /**
     * Saves the final results of a game session, including the session itself,
     * players, slides, and answers.
     *
     * @param sessionData The DTO containing all finalization data for the session.
     * @return The UUID of the saved GameSession.
     */
    String saveSessionFinalization(SessionFinalizationDto sessionData);
}