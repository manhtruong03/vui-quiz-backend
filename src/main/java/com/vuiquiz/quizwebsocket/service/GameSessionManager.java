package com.vuiquiz.quizwebsocket.service; // Use your actual package name

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet; // Thread-safe Set
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GameSessionManager {

    // <GamePin, SessionInfo>
    private final Map<String, SessionInfo> activeSessions = new ConcurrentHashMap<>();

    @Getter
    public static class SessionInfo {
        private final String gamePin;
        @Setter
        private volatile String hostSessionId = null; // WebSocket session ID of the host
        // Store only session IDs
        private final Set<String> playerSessionIds = new CopyOnWriteArraySet<>();

        public SessionInfo(String gamePin) {
            this.gamePin = gamePin;
        }

        public boolean isHostSet() {
            return hostSessionId != null;
        }

        public boolean isHost(String sessionId) {
            return hostSessionId != null && hostSessionId.equals(sessionId);
        }

        public boolean isPlayer(String sessionId) {
            return playerSessionIds.contains(sessionId);
        }

        // Add player ID if they are not the host
        public void addPlayer(String sessionId) {
            if (!isHost(sessionId)) {
                if(playerSessionIds.add(sessionId)) {
                    log.info("Session {}: Added player {}", gamePin, sessionId);
                }
            }
        }

        // Removes participant ID (player or host), returns true if found and removed/cleared
        public boolean removeParticipant(String sessionId) {
            boolean removedPlayer = playerSessionIds.remove(sessionId);
            if (removedPlayer) {
                log.info("Session {}: Removed player {}", gamePin, sessionId);
                return true;
            }
            // Check if it was the host being removed
            if (isHost(sessionId)) {
                log.warn("Session {}: Host {} disconnected.", gamePin, sessionId);
                this.hostSessionId = null; // Clear host reference
                return true; // Indicate a participant role was cleared
            }
            return false;
        }

        public int getPlayerCount() {
            return playerSessionIds.size();
        }

        // Get Session IDs of all players
        public Set<String> getAllPlayerSessionIds() {
            return new CopyOnWriteArraySet<>(playerSessionIds); // Return a copy
        }

        // Get all participant IDs (players + host if set) EXCEPT the sender
        public Set<String> getOtherParticipantIds(String senderSessionId) {
            Set<String> recipients = new CopyOnWriteArraySet<>(playerSessionIds);
            if (hostSessionId != null && !hostSessionId.equals(senderSessionId)) {
                recipients.add(hostSessionId);
            }
            // Ensure sender isn't in the recipient list (might be host)
            recipients.remove(senderSessionId);
            return recipients;
        }
    }

    // --- Service Methods ---

    public String createSession() {
        String gamePin;
        do {
            gamePin = String.format("%06d", ThreadLocalRandom.current().nextInt(100000, 1000000));
        } while (activeSessions.containsKey(gamePin));

        SessionInfo sessionInfo = new SessionInfo(gamePin);
        activeSessions.put(gamePin, sessionInfo);
        log.info("Created placeholder for game session with PIN: {}", gamePin);
        return gamePin;
    }

    public SessionInfo getSession(String gamePin) {
        return activeSessions.get(gamePin);
    }

    /**
     * Attempts to set the host for a session if it hasn't been set yet.
     * @param gamePin The game pin.
     * @param potentialHostSessionId The WebSocket session ID.
     * @return true if this session ID was assigned as host, false otherwise.
     */
    public boolean trySetHost(String gamePin, String potentialHostSessionId) {
        SessionInfo session = activeSessions.get(gamePin);
        if (session != null && !session.isHostSet()) {
            synchronized (session) {
                if (!session.isHostSet()) {
                    session.setHostSessionId(potentialHostSessionId);
                    // Note: We don't add host to playerSessionIds
                    log.info("Session {}: Host assigned to {}", gamePin, potentialHostSessionId);
                    return true; // Successfully assigned as host
                }
            }
        }
        if (session != null && session.isHost(potentialHostSessionId)){
            // Already the host, maybe reconnected? Return true conceptually.
            return true;
        }
        log.warn("Session {}: Failed to set host {} (host already set or session not found)", gamePin, potentialHostSessionId);
        return false; // Not assigned as host this time
    }


    /**
     * Adds a participant (player) to the session's player list.
     * @param gamePin The game pin.
     * @param participantSessionId The WebSocket session ID.
     */
    public void addParticipant(String gamePin, String participantSessionId) {
        SessionInfo session = activeSessions.get(gamePin);
        if (session != null) {
            // AddParticipant will check if it's the host and only add to player list if not
            session.addPlayer(participantSessionId);
        } else {
            log.warn("Attempt to add participant {} to non-existent session {}", participantSessionId, gamePin);
        }
    }


    /**
     * Removes a participant by session ID and handles session cleanup if needed.
     * @param sessionId The WebSocket session ID.
     * @return The gamePin of the session the participant was removed from, or null.
     */
    public String removeParticipant(String sessionId) {
        String sessionPinRemovedFrom = null;
        GameSessionManager.SessionInfo sessionConcerned = null;
        boolean hostDisconnected = false;

        for (Map.Entry<String, SessionInfo> entry : activeSessions.entrySet()) {
            SessionInfo session = entry.getValue();
            if (session.isHost(sessionId) || session.isPlayer(sessionId)) {
                sessionPinRemovedFrom = entry.getKey();
                sessionConcerned = session;
                hostDisconnected = session.isHost(sessionId); // Check if it *was* the host before removing
                session.removeParticipant(sessionId); // Removes from set OR clears hostSessionId
                break;
            }
        }

        if (sessionConcerned != null) {
            log.info("Participant {} removed from session {}", sessionId, sessionPinRemovedFrom);
            // Check if the session should be removed entirely
            // Remove if host is now null AND no players remain
            if (sessionConcerned.getHostSessionId() == null && sessionConcerned.getPlayerCount() == 0) {
                log.info("Session {} is now empty and hostless, removing.", sessionPinRemovedFrom);
                activeSessions.remove(sessionPinRemovedFrom);
                // Return the pin even if session was removed, listener might need it
            } else if (hostDisconnected) {
                log.warn("Session {} is now hostless.", sessionPinRemovedFrom);
                // Session persists but has no host
            }
        } else {
            log.debug("Participant {} not found in any active session upon disconnect.", sessionId);
        }
        return sessionPinRemovedFrom;
    }

    public String findGamePinBySessionId(String sessionId) {
        for (Map.Entry<String, SessionInfo> entry : activeSessions.entrySet()) {
            SessionInfo session = entry.getValue();
            if (session.isHost(sessionId) || session.isPlayer(sessionId)) {
                return entry.getKey();
            }
        }
        return null;
    }
}