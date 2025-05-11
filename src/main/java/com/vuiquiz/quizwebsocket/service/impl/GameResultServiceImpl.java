// src/main/java/com/vuiquiz/quizwebsocket/service/impl/GameResultServiceImpl.java
package com.vuiquiz.quizwebsocket.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vuiquiz.quizwebsocket.dto.SessionFinalizationDto;
import com.vuiquiz.quizwebsocket.dto.SessionGameSlideDto;
import com.vuiquiz.quizwebsocket.dto.SessionPlayerAnswerDto;
import com.vuiquiz.quizwebsocket.dto.SessionPlayerDto;
import com.vuiquiz.quizwebsocket.model.GameSession;
import com.vuiquiz.quizwebsocket.model.GameSlide;
import com.vuiquiz.quizwebsocket.model.Player;
import com.vuiquiz.quizwebsocket.model.PlayerAnswer; // Added for Phase 4
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
    @Transactional
    public String saveSessionFinalization(SessionFinalizationDto sessionData) {
        log.info("Attempting to save session finalization for gamePin: {}", sessionData.getGamePin());

        // Phase 1: Save GameSession
        GameSession gameSessionEntity = mapDtoToGameSession(sessionData);
        UUID hostIdToSet = determineAndValidateHostId(sessionData.getHostUserId());
        gameSessionEntity.setHostId(hostIdToSet);
        validateAndSetQuizId(gameSessionEntity, sessionData.getQuizId());
        GameSession savedGameSession = gameSessionRepository.save(gameSessionEntity);
        log.info("Successfully saved GameSession with ID: {} for gamePin: {}", savedGameSession.getSessionId(), savedGameSession.getGamePin());

        // --- Player ID Lookup Map Strategy ---
        // Fetch all players for this session once and create a map for quick lookup.
        Map<String, Player> playerClientIdToPlayerMap = sessionData.getPlayers().stream()
                .map(playerDto -> mapDtoToPlayer(playerDto, savedGameSession.getSessionId()))
                .peek(playerRepository::save) // Save each player as it's mapped
                .collect(Collectors.toMap(Player::getClientId, Function.identity()));
        log.info("Successfully mapped and saved {} players for session ID: {}", playerClientIdToPlayerMap.size(), savedGameSession.getSessionId());
        // Note: The above saves players one by one within the stream's peek.
        // If `saveAll` is preferred for players, map them first, then saveAll, then build the map from the saved list.
        // For simplicity here, peek is used. For very large numbers of players, batching `saveAll` after mapping might be better.

        // Phase 3 & 4: Save GameSlides and their PlayerAnswers
        if (!CollectionUtils.isEmpty(sessionData.getGameSlides())) {
            List<GameSlide> gameSlidesToSave = new ArrayList<>();
            List<PlayerAnswer> allPlayerAnswersToSave = new ArrayList<>();

            for (SessionGameSlideDto slideDto : sessionData.getGameSlides()) {
                GameSlide slideEntity = mapDtoToGameSlide(slideDto, savedGameSession.getSessionId());
                // It's important to save the slideEntity first to get its generated slideId
                GameSlide savedSlideEntity = gameSlideRepository.save(slideEntity);
                // gameSlidesToSave.add(savedSlideEntity); // Collect if batch save of slides is done at the end

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
            // gameSlideRepository.saveAll(gameSlidesToSave); // If collecting and saving slides at the end
            if (!allPlayerAnswersToSave.isEmpty()) {
                playerAnswerRepository.saveAll(allPlayerAnswersToSave);
                log.info("Successfully saved {} player answers for session ID: {}", allPlayerAnswersToSave.size(), savedGameSession.getSessionId());
            }
        } else {
            log.info("No game slides found in the payload for session ID: {}", savedGameSession.getSessionId());
        }

        return savedGameSession.getSessionId().toString();
    }

    private UUID determineAndValidateHostId(String hostUserIdFromDto) {
        UUID hostIdToSet;
        try {
            UUID parsedHostIdFromDto = UUID.fromString(hostUserIdFromDto);
            if (userAccountRepository.existsById(parsedHostIdFromDto)) {
                hostIdToSet = parsedHostIdFromDto;
                log.debug("Successfully validated hostUserId from DTO: {}", hostIdToSet);
            } else {
                log.warn("hostUserId from DTO '{}' (parsed as UUID '{}') does not exist. Falling back to authenticated user.", hostUserIdFromDto, parsedHostIdFromDto);
                hostIdToSet = getAuthenticatedUserId();
            }
        } catch (IllegalArgumentException e) {
            log.warn("hostUserId from DTO '{}' is not a valid UUID. Falling back to authenticated user. Error: {}", hostUserIdFromDto, e.getMessage());
            hostIdToSet = getAuthenticatedUserId();
        }

        if (hostIdToSet == null) {
            log.error("Could not determine host ID. Authenticated user ID is null, and DTO value was invalid/not found.");
            throw new IllegalStateException("Host ID could not be determined. Authenticated user is required if DTO value is invalid.");
        }
        return hostIdToSet;
    }

    private void validateAndSetQuizId(GameSession gameSession, String quizIdFromDto) {
        try {
            UUID quizId = UUID.fromString(quizIdFromDto);
            if (!quizRepository.existsById(quizId)) {
                log.error("Quiz with ID {} not found.", quizId);
                throw new IllegalArgumentException("Quiz not found for ID: " + quizId);
            }
            gameSession.setQuizId(quizId);
        } catch (IllegalArgumentException e) {
            log.error("Invalid quizId format: {}. Details: {}", quizIdFromDto, e.getMessage());
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