package com.vuiquiz.quizwebsocket.controller;

import com.vuiquiz.quizwebsocket.service.GameSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/session")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin // Allow requests from frontend (adjust origin in production)
public class GameSessionController {

    private final GameSessionManager sessionManager;

    /**
     * API to create a placeholder for a new WebSocket game session.
     * The first client connecting via WebSocket for this pin will become the host.
     * @return A map containing the generated gamePin.
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, String>> createSession() {
        // CORRECTED: Call createSession() without arguments, as defined in the refactored GameSessionManager
        String gamePin = sessionManager.createSession();
        log.info("API: /api/session/create called, generated gamePin: {}", gamePin);
        return ResponseEntity.ok(Map.of("gamePin", gamePin));
    }

    /**
     * API for a player to indicate intent to join a session.
     * In a simple setup, this might just validate the pin.
     * A more complex setup could return WebSocket endpoint details.
     * @param gamePin The game pin the player wants to join.
     * @param payload Map containing player 'nickname'.
     * @return HTTP Status indicating success or failure.
     */
    @PostMapping("/join/{gamePin}")
    public ResponseEntity<Map<String, String>> joinSession(@PathVariable String gamePin, @RequestBody Map<String, String> payload) {
        log.info("API: /api/session/join/{} called with payload: {}", gamePin, payload);
        GameSessionManager.SessionInfo session = sessionManager.getSession(gamePin);
        String nickname = payload.get("nickname");

        if (session == null) {
            log.warn("Join attempt failed: Session with PIN {} not found.", gamePin);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Game session not found."));
        }
        if (nickname == null || nickname.trim().isEmpty()) {
            log.warn("Join attempt failed for PIN {}: Nickname is required.", gamePin);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Nickname is required."));
        }

        // Here, we just acknowledge the intent. The actual joining happens via WebSocket connect.
        // You could add nickname validation (uniqueness within session) here if desired.
        log.info("Player '{}' validated for joining session PIN {}", nickname, gamePin);
        // Return success and potentially the WebSocket endpoint URL
        return ResponseEntity.ok(Map.of(
                "message", "Session found. Connect via WebSocket.",
                "websocketEndpoint", "/ws-quiz" // Inform client where to connect
        ));
    }
}