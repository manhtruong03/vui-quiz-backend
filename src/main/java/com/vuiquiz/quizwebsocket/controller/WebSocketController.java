package com.vuiquiz.quizwebsocket.controller;

import com.vuiquiz.quizwebsocket.service.websocket.GameSessionManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference; // Import TypeReference
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload; // Use @Payload for the message body
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final GameSessionManager sessionManager;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper; // For JSON manipulation

    // Define a TypeReference for parsing the expected message structure
    // Based on docs/websocket_message_structure.txt
    private static final TypeReference<List<Map<String, Object>>> MESSAGE_LIST_TYPE_REF = new TypeReference<>() {};

    /**
     * Handles messages sent to /app/controller/{gamepin}.
     * Acts as a relay, adding sender's CID and forwarding the message.
     *
     * @param gamepin The game pin from the destination path.
     * @param messagePayload The raw message payload (expected to be a JSON string, potentially an array).
     * @param headerAccessor Accessor for message headers (contains session ID).
     */
    @MessageMapping("/controller/{gamepin}")
    public void relayGameAction(@DestinationVariable String gamepin,
                                @Payload String messagePayload, // Receive raw payload
                                SimpMessageHeaderAccessor headerAccessor) {

        String senderSessionId = headerAccessor.getSessionId();
        if (senderSessionId == null) {
            log.error("Cannot process message without sender session ID for gamepin {}", gamepin);
            return;
        }

        log.debug("Received raw message on /app/controller/{} from {}: {}", gamepin, senderSessionId, messagePayload);

        GameSessionManager.SessionInfo session = sessionManager.getSession(gamepin);
        if (session == null) {
            log.warn("Received message for non-existent session PIN: {}. Sender: {}", gamepin, senderSessionId);
            // Optional: Send error back to sender
            // messagingTemplate.convertAndSendToUser(senderSessionId, "/queue/errors", "{\"error\":\"Session not found\"}", createHeaders(senderSessionId));
            return;
        }

        // Determine if the sender is the host
        boolean isHostMessage = session.isHost(senderSessionId);
        String senderRole = isHostMessage ? "HOST" : "PLAYER";

        try {
            // --- Parse, Modify, Re-serialize ---
            // The docs show messages often wrapped in an array, e.g., [{...}]
            // We need to handle this structure.
            List<Map<String, Object>> messageList = objectMapper.readValue(messagePayload, MESSAGE_LIST_TYPE_REF);

            if (messageList == null || messageList.isEmpty()) {
                log.warn("Received empty or invalid message structure from {} for session {}", senderSessionId, gamepin);
                return;
            }

            // Process each message in the list (usually just one)
            for (Map<String, Object> message : messageList) {
                // Add/Update the 'cid' (Client ID) in the 'data' part of the message
                // Ensure 'data' exists and is a Map
                Object dataObj = message.get("data");
                if (dataObj instanceof Map) {
                    @SuppressWarnings("unchecked") // Necessary cast
                    Map<String, Object> dataMap = (Map<String, Object>) dataObj;
                    dataMap.put("cid", senderSessionId); // Add sender's WebSocket Session ID as 'cid'
                } else {
                    log.warn("Message from {} for session {} is missing 'data' map or has incorrect format. Payload: {}", senderSessionId, gamepin, message);
                    // Optionally create the data map if strictly needed, but indicates client error
                    // Map<String, Object> newDataMap = new HashMap<>();
                    // newDataMap.put("cid", senderSessionId);
                    // message.put("data", newDataMap);
                    continue; // Skip this message if format is wrong
                }

                // Add sender role if needed by clients (Optional)
                // message.put("senderRole", senderRole);

                // Re-serialize the modified message object (just this one element for sending)
                String modifiedPayload = objectMapper.writeValueAsString(message); // Send single modified object

                // --- Relay Logic ---
                if (isHostMessage) {
                    // Host sends message -> Relay to all players on the player topic
                    String destination = "/topic/player/" + gamepin;
                    log.info("Relaying message from HOST {} to Players [{}]: {}", senderSessionId, destination, modifiedPayload);
                    messagingTemplate.convertAndSend(destination, modifiedPayload);
                } else {
                    // Player sends message -> Relay ONLY to the host on the host topic
                    String hostSessionId = session.getHostSessionId();
                    if (hostSessionId != null) {
                        String destination = "/topic/host/" + gamepin; // Topic specific to the host of this game
                        log.info("Relaying message from PLAYER {} to Host {} [{}]: {}", senderSessionId, hostSessionId, destination, modifiedPayload);
                        // The host client needs to subscribe to /topic/host/{gamepin}
                        messagingTemplate.convertAndSend(destination, modifiedPayload);
                        // Alternatively, send directly to the host user if user destinations are set up:
                        // messagingTemplate.convertAndSendToUser(hostSessionId, "/queue/player-messages", modifiedPayload, createHeaders(hostSessionId));
                    } else {
                        log.warn("Player {} sent message for session {} but host is not connected.", senderSessionId, gamepin);
                        // Optional: Notify player that host is unavailable
                    }
                }
            } // End loop through messages in list

        } catch (JsonProcessingException e) {
            log.error("Failed to parse/serialize message from {} for session {}: {}", senderSessionId, gamepin, e.getMessage());
            // Optional: Send error back to sender
        } catch (ClassCastException e) {
            log.error("Failed processing message structure from {} for session {}: {}. Payload: {}", senderSessionId, gamepin, e.getMessage(), messagePayload);
            // Optional: Send error back to sender
        } catch (Exception e) { // Catch other potential errors
            log.error("Unexpected error relaying message from {} for session {}: {}", senderSessionId, gamepin, e.getMessage(), e);
        }
    }

    // Optional Helper: createHeaders for sendToUser (if needed)
    /*
    private MessageHeaders createHeaders(String sessionId) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headerAccessor.setSessionId(sessionId);
        headerAccessor.setLeaveMutable(true);
        return headerAccessor.getMessageHeaders();
    }
    */
}