package com.vuiquiz.quizwebsocket.listener;

import com.vuiquiz.quizwebsocket.service.GameSessionManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// Import specific event types directly
import org.springframework.context.ApplicationListener; // Keep general ApplicationListener
import org.springframework.context.event.EventListener; // Alternatively, use @EventListener annotation
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;


import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
// Implement ApplicationListener separately or use @EventListener annotations
public class WebSocketEventListener {

    private final GameSessionManager sessionManager;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Pattern to extract gamePin from destinations
    private static final Pattern GAME_PIN_PATTERN = Pattern.compile(".*/(player|host|controller)/(\\d{6})(?:/.*)?");

    // Option 1: Use @EventListener annotation (often preferred)
    @EventListener
    public void handleWebSocketConnectListener(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination();

        if (sessionId == null || destination == null) {
            log.warn("Received subscribe event with missing sessionId or destination.");
            return;
        }

        log.debug("Session {} subscribed to destination: {}", sessionId, destination);

        String gamePin = extractGamePin(destination);

        if (gamePin != null) {
            GameSessionManager.SessionInfo session = sessionManager.getSession(gamePin);
            if (session == null) {
                log.warn("Session {} subscribed to destination for non-existent game pin: {}", sessionId, gamePin);
                // Send error back to client?
                // messagingTemplate.convertAndSendToUser(sessionId, "/queue/errors", "Invalid game pin", createHeaders(sessionId));
                return;
            }

            boolean isNewHost = false;
            if (!session.isHostSet()) {
                isNewHost = sessionManager.trySetHost(gamePin, sessionId);
                if (isNewHost) {
                    log.info("Session {}: First participant {} assigned as HOST.", gamePin, sessionId);
                    sendConfirmation(sessionId, true);
                } else {
                    log.info("Session {}: Host already set, adding {} as player.", gamePin, sessionId);
                    sessionManager.addParticipant(gamePin, sessionId);
                    sendConfirmation(sessionId, false);
                }
            } else {
                log.debug("Session {}: Adding participant {}.", gamePin, sessionId);
                sessionManager.addParticipant(gamePin, sessionId);
                if (!session.isHost(sessionId)) {
                    sendConfirmation(sessionId, false);
                }
            }
            notifyPlayerUpdate(gamePin, sessionId, "PARTICIPANT_JOINED");
        } else {
            log.debug("Subscription by {} to non-game destination: {}", sessionId, destination);
        }
    }

    // Option 1: Use @EventListener annotation
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        if (sessionId == null) {
            log.error("Disconnect event received with null session ID.");
            return;
        }

        log.info("WebSocket session disconnected: {}", sessionId);
        String gamePin = sessionManager.removeParticipant(sessionId);

        if (gamePin != null) {
            GameSessionManager.SessionInfo session = sessionManager.getSession(gamePin);
            notifyPlayerUpdate(gamePin, sessionId, "PARTICIPANT_LEFT");
            if (session == null) {
                log.info("Session {} was removed because the host or last player disconnected.", gamePin);
            }
        } else {
            log.debug("Disconnected session {} was not found in any active game.", sessionId);
        }
    }


    // --- Helper Methods remain the same ---

    private String extractGamePin(String destination) {
        if (destination == null) return null;
        Matcher matcher = GAME_PIN_PATTERN.matcher(destination);
        if (matcher.matches()) {
            return matcher.group(2);
        }
        log.trace("Could not extract game pin from destination: {}", destination);
        return null;
    }

    private void sendConfirmation(String sessionId, boolean isHost) {
        Map<String, Object> payload = Map.of(
                "type", isHost ? "HOST_ASSIGNED" : "PLAYER_ASSIGNED",
                "isHost", isHost,
                "clientId", sessionId // Also send back the client's ID
        );
        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            // Send to a user-specific queue
            messagingTemplate.convertAndSendToUser(sessionId, "/queue/private", jsonPayload, createHeaders(sessionId));
            log.info("Sent {} confirmation to session {}", (isHost ? "Host" : "Player"), sessionId);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize confirmation message for session {}: {}", sessionId, e.getMessage());
        } catch (Exception e) {
            log.error("Failed to send confirmation to session {}: {}", sessionId, e.getMessage());
        }
    }


    private void notifyPlayerUpdate(String gamePin, String affectedSessionId, String eventType) {
        GameSessionManager.SessionInfo session = sessionManager.getSession(gamePin);
        if (session == null) {
            log.debug("Cannot notify player update for already removed session {}", gamePin);
            return;
        }

        Map<String, Object> updatePayload = Map.of(
                "type", eventType,
                "playerCount", session.getPlayerCount(),
                "hostId", Objects.toString(session.getHostSessionId(),""),
                "affectedId", Objects.toString(affectedSessionId,"")
                // Add more info like nicknames if clients need it
        );

        try {
            String jsonPayload = objectMapper.writeValueAsString(updatePayload);

            // Notify players topic
            String playerTopic = "/topic/player/" + gamePin;
            log.info("Notifying players on {}: {}", playerTopic, jsonPayload);
            messagingTemplate.convertAndSend(playerTopic, jsonPayload);

            // Notify host topic
            String hostId = session.getHostSessionId();
            if (hostId != null) {
                String hostTopic = "/topic/host/" + gamePin;
                log.info("Notifying host on {}: {}", hostTopic, jsonPayload);
                messagingTemplate.convertAndSend(hostTopic, jsonPayload);
            }

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize player update message for session {}: {}", gamePin, e.getMessage());
        } catch (Exception e) {
            log.error("Failed to send player update for session {}: {}", gamePin, e.getMessage());
        }
    }

    private MessageHeaders createHeaders(String sessionId) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headerAccessor.setSessionId(sessionId);
        headerAccessor.setLeaveMutable(true);
        return headerAccessor.getMessageHeaders();
    }
}