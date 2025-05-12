// src/main/java/com/vuiquiz/quizwebsocket/service/impl/GameResultServiceImpl.java
package com.vuiquiz.quizwebsocket.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vuiquiz.quizwebsocket.dto.SessionFinalizationDto;
import com.vuiquiz.quizwebsocket.dto.SessionGameSlideDto;
import com.vuiquiz.quizwebsocket.dto.SessionPlayerAnswerDto;
import com.vuiquiz.quizwebsocket.dto.SessionPlayerDto;
import com.vuiquiz.quizwebsocket.exception.ForbiddenAccessException;
import com.vuiquiz.quizwebsocket.exception.ResourceNotFoundException;
import com.vuiquiz.quizwebsocket.exception.UnauthorizedException;
import com.vuiquiz.quizwebsocket.model.*;
import com.vuiquiz.quizwebsocket.repository.*; // Assuming PlayerAnswerRepository is here
import com.vuiquiz.quizwebsocket.security.services.UserDetailsImpl;
import com.vuiquiz.quizwebsocket.service.GameResultService;
import com.vuiquiz.quizwebsocket.utils.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameResultServiceImpl implements GameResultService {

    private final GameSessionRepository gameSessionRepository;
    private final UserAccountRepository userAccountRepository;
    private final QuizRepository quizRepository;
    private final PlayerRepository playerRepository;
    private final GameSlideRepository gameSlideRepository;
    private final PlayerAnswerRepository playerAnswerRepository; // Added for Phase 4
    // private final QuestionRepository questionRepository;
    // private final PowerUpRepository powerUpRepository; // For future validation of usedPowerUpId
    private final ObjectMapper objectMapper;

    @Override
    @Transactional // This transaction now includes updating the Quiz
    public String saveSessionFinalization(SessionFinalizationDto sessionData) {
        log.info("Attempting to save session finalization for gamePin: {}", sessionData.getGamePin());

        UUID authenticatedUserId = getAuthenticatedUserId();
        if (authenticatedUserId == null) {
            log.error("User performing finalize operation is not authenticated or user details not found.");
            throw new UnauthorizedException("User must be authenticated to finalize a session.");
        }

        // Phase 1: Save GameSession
        GameSession gameSessionEntity = mapDtoToGameSession(sessionData);
        UUID actualHostId = determineAndValidateHostId(sessionData.getHostUserId(), authenticatedUserId);
        gameSessionEntity.setHostId(actualHostId);

        UUID quizUuid = validateAndSetQuizId(gameSessionEntity, sessionData.getQuizId()); // Get the validated Quiz UUID

        GameSession savedGameSession = gameSessionRepository.save(gameSessionEntity);
        log.info("Successfully saved GameSession with ID: {} for gamePin: {}", savedGameSession.getSessionId(), savedGameSession.getGamePin());

        Map<String, Player> playerClientIdToPlayerMap = savePlayers(sessionData.getPlayers(), savedGameSession.getSessionId());

        saveGameSlidesAndAnswers(sessionData.getGameSlides(), savedGameSession.getSessionId(), playerClientIdToPlayerMap);

        // --- TWEAK 1: Update Quiz play_count and status ---
        try {
            Quiz quizToUpdate = quizRepository.findById(quizUuid)
                    .orElseThrow(() -> new ResourceNotFoundException("Quiz", "id", quizUuid)); // Should not happen if validated above

            quizToUpdate.setPlayCount(quizToUpdate.getPlayCount() + 1);
            if ("DRAFT".equalsIgnoreCase(quizToUpdate.getStatus())) {
                quizToUpdate.setStatus("PUBLISHED");
                log.info("Quiz ID: {} status updated from DRAFT to PUBLISHED.", quizUuid);
            }
            quizRepository.save(quizToUpdate);
            log.info("Quiz ID: {} play_count incremented to {}.", quizUuid, quizToUpdate.getPlayCount());
        } catch (Exception e) {
            // Log the error but don't let it fail the whole session finalization if this part fails.
            // This is a secondary operation. Alternatively, if it MUST succeed, remove the try-catch.
            log.error("Error updating play_count or status for Quiz ID {}: {}", quizUuid, e.getMessage(), e);
        }
        // --- END TWEAK 1 ---

        return savedGameSession.getSessionId().toString();
    }

    private UUID determineAndValidateHostId(String hostUserIdFromDto, UUID authenticatedUserId) {
        UUID hostIdToSet;
        boolean dtoHostIdValidAndExists = false;
        UUID parsedHostIdFromDto = null;

        if (StringUtils.hasText(hostUserIdFromDto)) {
            try {
                parsedHostIdFromDto = UUID.fromString(hostUserIdFromDto);
                if (userAccountRepository.existsById(parsedHostIdFromDto)) {
                    dtoHostIdValidAndExists = true;
                } else {
                    log.warn("hostUserId from DTO '{}' (parsed as UUID '{}') does not exist in user_account table.", hostUserIdFromDto, parsedHostIdFromDto);
                }
            } catch (IllegalArgumentException e) {
                log.warn("hostUserId from DTO '{}' is not a valid UUID format. Error: {}", hostUserIdFromDto, e.getMessage());
            }
        } else {
            log.warn("hostUserId from DTO is blank or null.");
        }

        if (dtoHostIdValidAndExists) {
            if (!parsedHostIdFromDto.equals(authenticatedUserId)) {
                log.error("Forbidden: Authenticated user {} attempted to finalize session for hostUserId {}.", authenticatedUserId, parsedHostIdFromDto);
                throw new ForbiddenAccessException("Authenticated user is not authorized to finalize a session for the specified host user.");
            }
            hostIdToSet = parsedHostIdFromDto;
            log.info("HostUserId from DTO ({}) matches authenticated user. Using this ID.", hostIdToSet);
        } else {
            log.warn("Falling back to authenticated user ID ({}) as the host for this session.", authenticatedUserId);
            hostIdToSet = authenticatedUserId;
        }

        if (hostIdToSet == null) {
            log.error("Critical error: Host ID could not be determined even after fallback. Authenticated user ID might be null.");
            throw new IllegalStateException("Host ID could not be determined.");
        }
        return hostIdToSet;
    }

    // Modified to return the UUID for convenience
    private UUID validateAndSetQuizId(GameSession gameSession, String quizIdFromDto) {
        if (!StringUtils.hasText(quizIdFromDto)) {
            log.error("quizId is missing in the request payload for gamePin: {}", gameSession.getGamePin());
            throw new IllegalArgumentException("quizId cannot be null or empty.");
        }
        try {
            UUID quizId = UUID.fromString(quizIdFromDto);
            if (!quizRepository.existsById(quizId)) {
                log.error("Quiz with ID {} not found for gamePin: {}", quizId, gameSession.getGamePin());
                throw new ResourceNotFoundException("Quiz", "id", quizIdFromDto);
            }
            gameSession.setQuizId(quizId);
            return quizId; // Return the validated UUID
        } catch (IllegalArgumentException e) {
            log.error("Invalid quizId format: {} for gamePin: {}. Details: {}", quizIdFromDto, gameSession.getGamePin(), e.getMessage());
            throw new IllegalArgumentException("Invalid quizId format: " + quizIdFromDto);
        }
    }

    private UUID getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            UUID userId = userDetails.getId();
            log.debug("Using authenticated user ID: {}", userId);
            return userId;
        }
        log.warn("Could not retrieve authenticated user ID. No authenticated principal found or principal is not UserDetailsImpl.");
        return null;
    }

    private Map<String, Player> savePlayers(List<SessionPlayerDto> playerDtos, UUID sessionId) {
        if (CollectionUtils.isEmpty(playerDtos)) {
            log.info("No players found in the payload for session ID: {}", sessionId);
            return Map.of();
        }
        List<Player> playersToSave = new ArrayList<>();
        for (SessionPlayerDto playerDto : playerDtos) {
            Player playerEntity = mapDtoToPlayer(playerDto, sessionId);
            playersToSave.add(playerEntity);
        }
        List<Player> savedPlayers = playerRepository.saveAll(playersToSave);
        log.info("Successfully saved {} players for session ID: {}", savedPlayers.size(), sessionId);
        return savedPlayers.stream().collect(Collectors.toMap(Player::getClientId, Function.identity()));
    }

    private void saveGameSlidesAndAnswers(List<SessionGameSlideDto> gameSlideDtos, UUID sessionId, Map<String, Player> playerClientIdToPlayerMap) {
        if (CollectionUtils.isEmpty(gameSlideDtos)) {
            log.info("No game slides found in the payload for session ID: {}", sessionId);
            return;
        }
        List<PlayerAnswer> allPlayerAnswersToSave = new ArrayList<>();
        for (SessionGameSlideDto slideDto : gameSlideDtos) {
            GameSlide slideEntity = mapDtoToGameSlide(slideDto, sessionId);
            GameSlide savedSlideEntity = gameSlideRepository.save(slideEntity);
            if (!CollectionUtils.isEmpty(slideDto.getPlayerAnswers())) {
                for (SessionPlayerAnswerDto answerDto : slideDto.getPlayerAnswers()) {
                    Player currentPlayer = playerClientIdToPlayerMap.get(answerDto.getClientId());
                    if (currentPlayer != null) {
                        PlayerAnswer answerEntity = mapDtoToPlayerAnswer(answerDto, savedSlideEntity.getSlideId(), currentPlayer.getPlayerId());
                        allPlayerAnswersToSave.add(answerEntity);
                    } else {
                        log.warn("Could not find player with clientId: {} for an answer on slideIndex: {}. Skipping this answer.",
                                answerDto.getClientId(), slideDto.getSlideIndex());
                    }
                }
            }
        }
        if (!allPlayerAnswersToSave.isEmpty()) {
            playerAnswerRepository.saveAll(allPlayerAnswersToSave);
            log.info("Successfully saved {} player answers for session ID: {}", allPlayerAnswersToSave.size(), sessionId);
        }
    }

    private GameSession mapDtoToGameSession(SessionFinalizationDto dto) {
        GameSession session = new GameSession();
        session.setGamePin(dto.getGamePin());
        session.setStartedAt(DateTimeUtil.fromMillis(dto.getSessionStartTime()));
        session.setEndedAt(DateTimeUtil.fromMillis(dto.getSessionEndTime()));
        session.setGameType(dto.getGameType());
        session.setPlayerCount(dto.getFinalPlayerCount());
        session.setStatus(dto.getFinalSessionStatus());
        session.setAllowLateJoin(dto.isAllowLateJoin());
        session.setPowerUpsEnabled(dto.isPowerUpsEnabled());
        session.setTerminationReason(dto.getTerminationReason());
        session.setTerminationSlideIndex(dto.getTerminationSlideIndex());
        return session;
    }

    private Player mapDtoToPlayer(SessionPlayerDto dto, UUID sessionId) {
        Player player = new Player();
        player.setSessionId(sessionId);
        player.setClientId(dto.getClientId());
        player.setNickname(dto.getNickname());

        if (StringUtils.hasText(dto.getUserId())) {
            try {
                UUID userUuid = UUID.fromString(dto.getUserId());
                player.setUserId(userUuid);
            } catch (IllegalArgumentException e) {
                log.warn("Player DTO contained invalid UUID format for userId: {}. Leaving Player.userId as null.", dto.getUserId());
            }
        }

        player.setStatus(dto.getStatus());
        player.setJoinedAt(DateTimeUtil.fromMillis(dto.getJoinedAt()));
        player.setJoinSlideIndex(dto.getJoinSlideIndex());
        player.setWaitingSince(DateTimeUtil.fromMillis(dto.getWaitingSince()));
        player.setRank(dto.getRank());
        player.setTotalScore(dto.getTotalScore());
        player.setCorrectAnswers(dto.getCorrectAnswers());
        player.setStreakCount(dto.getStreakCount());
        player.setAnswerCount(dto.getAnswerCount());
        player.setUnansweredCount(dto.getUnansweredCount());
        player.setTotalTime(dto.getTotalTime());
        player.setLastActivityAt(DateTimeUtil.fromMillis(dto.getLastActivityAt()));

        if (dto.getDeviceInfoJson() != null && !dto.getDeviceInfoJson().isNull()) {
            try {
                player.setDeviceInfoJson(objectMapper.writeValueAsString(dto.getDeviceInfoJson()));
            } catch (JsonProcessingException e) {
                log.error("Error serializing deviceInfoJson for player {}: {}", dto.getNickname(), e.getMessage());
                player.setDeviceInfoJson(null);
            }
        }

        if (StringUtils.hasText(dto.getAvatarId())) {
            try {
                UUID avatarUuid = UUID.fromString(dto.getAvatarId());
                player.setAvatarId(avatarUuid);
            } catch (IllegalArgumentException e) {
                log.warn("Player DTO contained invalid UUID format for avatarId: {}. Leaving Player.avatarId as null.", dto.getAvatarId());
            }
        }

        if (dto.getAnswerCount() != null && dto.getAnswerCount() > 0 && dto.getTotalTime() != null) {
            player.setAverageTime((int) (dto.getTotalTime() / dto.getAnswerCount()));
        } else {
            player.setAverageTime(0);
        }
        return player;
    }

    private GameSlide mapDtoToGameSlide(SessionGameSlideDto dto, UUID sessionId) {
        GameSlide slide = new GameSlide();
        slide.setSessionId(sessionId);
        slide.setSlideIndex(dto.getSlideIndex());
        slide.setSlideType(dto.getSlideType());
        slide.setStatus(dto.getStatus());
        slide.setStartedAt(DateTimeUtil.fromMillis(dto.getStartedAt()));
        slide.setEndedAt(DateTimeUtil.fromMillis(dto.getEndedAt()));

        if (StringUtils.hasText(dto.getOriginalQuestionId())) {
            try {
                UUID questionUuid = UUID.fromString(dto.getOriginalQuestionId());
                slide.setOriginalQuestionId(questionUuid);
            } catch (IllegalArgumentException e) {
                log.warn("GameSlide DTO contained invalid UUID format for originalQuestionId: {}. Leaving GameSlide.originalQuestionId as null.", dto.getOriginalQuestionId());
            }
        }

        if (dto.getQuestionDistributionJson() != null && !dto.getQuestionDistributionJson().isNull()) {
            try {
                slide.setQuestionDistributionJson(objectMapper.writeValueAsString(dto.getQuestionDistributionJson()));
            } catch (JsonProcessingException e) {
                log.error("Error serializing questionDistributionJson for slideIndex {}: {}", dto.getSlideIndex(), e.getMessage());
                slide.setQuestionDistributionJson(null);
            }
        }
        return slide;
    }

    private PlayerAnswer mapDtoToPlayerAnswer(SessionPlayerAnswerDto dto, UUID slideId, UUID playerId) {
        PlayerAnswer answer = new PlayerAnswer();
        // answer_id is generated by DB
        answer.setSlideId(slideId);   // Link to the GameSlide
        answer.setPlayerId(playerId); // Link to the Player

        // Handle 'choice' (JsonNode to String)
        if (dto.getChoice() != null && !dto.getChoice().isNull()) {
            if (dto.getChoice().isNumber()) {
                answer.setChoice(dto.getChoice().asText()); // Store single number as string
            } else if (dto.getChoice().isArray()) {
                try {
                    answer.setChoice(objectMapper.writeValueAsString(dto.getChoice())); // Store array as JSON string
                } catch (JsonProcessingException e) {
                    log.error("Error serializing player answer 'choice' array for player ID {}: {}", playerId, e.getMessage());
                    answer.setChoice(null);
                }
            } else { // Could be text node if single choice was sent as string in JSON, or other types.
                answer.setChoice(dto.getChoice().asText()); // Default to asText()
            }
        }

        answer.setText(dto.getText());
        answer.setReactionTimeMs(dto.getReactionTimeMs());
        answer.setAnswerTimestamp(DateTimeUtil.fromMillis(dto.getAnswerTimestamp()));
        answer.setStatus(dto.getStatus());
        answer.setBasePoints(dto.getBasePoints());
        answer.setFinalPoints(dto.getFinalPoints());

        // Handle usedPowerUpId (String to UUID)
        if (StringUtils.hasText(dto.getUsedPowerUpId())) {
            try {
                UUID powerUpUuid = UUID.fromString(dto.getUsedPowerUpId());
                // Optionally validate existence:
                // if(powerUpRepository.existsById(powerUpUuid)) { // Requires PowerUpRepository
                //    answer.setUsedPowerUpId(powerUpUuid);
                // } else {
                //    log.warn("PlayerAnswer DTO contained usedPowerUpId {} but no such power-up exists.", powerUpUuid);
                // }
                answer.setUsedPowerUpId(powerUpUuid);
            } catch (IllegalArgumentException e) {
                log.warn("PlayerAnswer DTO contained invalid UUID format for usedPowerUpId: {}.", dto.getUsedPowerUpId());
            }
        }

        // Handle usedPowerUpContextJson (JsonNode to String)
        if (dto.getUsedPowerUpContextJson() != null && !dto.getUsedPowerUpContextJson().isNull()) {
            try {
                answer.setUsedPowerUpContextJson(objectMapper.writeValueAsString(dto.getUsedPowerUpContextJson()));
            } catch (JsonProcessingException e) {
                log.error("Error serializing usedPowerUpContextJson for player ID {}: {}", playerId, e.getMessage());
                answer.setUsedPowerUpContextJson(null);
            }
        }
        return answer;
    }
}