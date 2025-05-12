package com.vuiquiz.quizwebsocket.controller;

import com.vuiquiz.quizwebsocket.service.websocket.GameSessionManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
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
    private final ObjectMapper objectMapper;

    private static final TypeReference<List<Map<String, Object>>> MESSAGE_LIST_TYPE_REF = new TypeReference<>() {};
    private static final String USER_QUEUE_PRIVATE_SUFFIX = "/queue/private"; // For client subscription
    private static final String BROKER_USER_QUEUE_PREFIX = "/queue/private-user"; // How SimpleBroker sees it

    @MessageMapping("/controller/{gamepin}")
    public void relayGameAction(@DestinationVariable String gamepin,
                                @Payload String messagePayload,
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
            return;
        }

        boolean isHostMessage = session.isHost(senderSessionId);

        try {
            List<Map<String, Object>> messageList = objectMapper.readValue(messagePayload, MESSAGE_LIST_TYPE_REF);

            if (messageList == null || messageList.isEmpty()) {
                log.warn("Received empty or invalid message structure from {} for session {}", senderSessionId, gamepin);
                return;
            }

            for (Map<String, Object> message : messageList) {
                Map<String, Object> dataMap = null;
                Object dataObj = message.get("data");

                if (dataObj instanceof Map) {
                    dataMap = (Map<String, Object>) dataObj;
                } else {
                    log.warn("Message from {} for session {} is missing 'data' map or has incorrect format. Original message: {}", senderSessionId, gamepin, message);
                    continue;
                }

                boolean processedPrivately = false;

                if (isHostMessage && dataMap != null) {
                    Object messageDataIdObj = dataMap.get("id");
                    Object targetPlayerCidObj = dataMap.get("cid");

                    if (messageDataIdObj instanceof Number && targetPlayerCidObj instanceof String) {
                        int msgId = ((Number) messageDataIdObj).intValue();
                        String targetPlayerCid = (String) targetPlayerCidObj; // This IS the STOMP session ID of the player

                        if (msgId == 8 || msgId == 13) {
                            String privateMessagePayloadJson = objectMapper.writeValueAsString(message); // Use the original message structure

                            // +++ MODIFICATION: Send directly to the resolved broker queue +++
                            String resolvedBrokerDestination = BROKER_USER_QUEUE_PREFIX + targetPlayerCid;

                            log.info("Relaying private message from HOST {} to Player {} on resolved broker destination: {}. Message: {}",
                                    senderSessionId, targetPlayerCid, resolvedBrokerDestination, privateMessagePayloadJson);

                            // Instead of convertAndSendToUser, use convertAndSend directly to the broker queue
                            // that the client (sub-1) is actually subscribed to.
                            messagingTemplate.convertAndSend(resolvedBrokerDestination, privateMessagePayloadJson);
                            // +++ END MODIFICATION +++

                            processedPrivately = true;
                        }
                    }
                }

                if (!processedPrivately) {
                    if (dataMap != null) {
                        dataMap.put("cid", senderSessionId);
                    } else {
                        log.error("DataMap is null when attempting to set sender's CID for non-private message. Session: {}, Sender: {}. Message: {}", gamepin, senderSessionId, message);
                        continue;
                    }
                    String modifiedPayload = objectMapper.writeValueAsString(message); // Use the original message map
                    if (isHostMessage) {
                        String destination = "/topic/player/" + gamepin;
                        log.info("Broadcasting message from HOST {} to Players [{}]: {}", senderSessionId, destination, modifiedPayload);
                        messagingTemplate.convertAndSend(destination, modifiedPayload);
                    } else {
                        String hostSessionId = session.getHostSessionId();
                        if (hostSessionId != null) {
                            String destination = "/topic/host/" + gamepin;
                            log.info("Relaying message from PLAYER {} to Host {} [{}]: {}", senderSessionId, hostSessionId, destination, modifiedPayload);
                            messagingTemplate.convertAndSend(destination, modifiedPayload);
                        } else {
                            log.warn("Player {} sent message for session {} but host is not connected.", senderSessionId, gamepin);
                        }
                    }
                }
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to parse/serialize message from {} for session {}: {}. Raw payload: {}", senderSessionId, gamepin, e.getMessage(), messagePayload);
        } catch (ClassCastException e) {
            log.error("Failed processing message structure (casting issue) from {} for session {}: {}. Raw payload: {}", senderSessionId, gamepin, e.getMessage(), messagePayload);
        } catch (Exception e) {
            log.error("Unexpected error relaying message from {} for session {}: {}", senderSessionId, gamepin, e.getMessage(), e);
        }
    }
}