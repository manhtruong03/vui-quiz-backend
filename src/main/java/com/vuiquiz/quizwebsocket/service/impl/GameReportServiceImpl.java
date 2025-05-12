// src/main/java/com/vuiquiz/quizwebsocket/service/impl/GameReportServiceImpl.java
package com.vuiquiz.quizwebsocket.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vuiquiz.quizwebsocket.dto.ChoiceDTO;
import com.vuiquiz.quizwebsocket.dto.QuestionDTO;
import com.vuiquiz.quizwebsocket.dto.report.*;
import com.vuiquiz.quizwebsocket.exception.ResourceNotFoundException;
import com.vuiquiz.quizwebsocket.exception.UnauthorizedException;
import com.vuiquiz.quizwebsocket.model.*;
import com.vuiquiz.quizwebsocket.repository.*;
import com.vuiquiz.quizwebsocket.security.services.UserDetailsImpl;
import com.vuiquiz.quizwebsocket.service.GameReportService;
import com.vuiquiz.quizwebsocket.utils.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameReportServiceImpl implements GameReportService {

    private final GameSessionRepository gameSessionRepository;
    private final QuizRepository quizRepository;
    private final UserAccountRepository userAccountRepository;
    private final PlayerRepository playerRepository;
    private final PlayerAnswerRepository playerAnswerRepository;
    private final GameSlideRepository gameSlideRepository;
    private final ObjectMapper objectMapper;


    @Override
    @Transactional(readOnly = true)
    public SessionSummaryDto getSessionSummary(UUID sessionId) {
        log.info("Fetching summary report for session ID: {}", sessionId);

        GameSession session = gameSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("GameSession", "id", sessionId));

        Quiz quiz = quizRepository.findById(session.getQuizId())
                .orElseThrow(() -> new ResourceNotFoundException("Quiz", "id", session.getQuizId()));

        UserAccount host = userAccountRepository.findById(session.getHostId())
                .orElseThrow(() -> new ResourceNotFoundException("UserAccount (Host)", "id", session.getHostId()));

        UserAccount quizCreator = null;
        if (quiz.getCreatorId() != null) {
            quizCreator = userAccountRepository.findById(quiz.getCreatorId()).orElse(null);
        }

        List<Player> players = playerRepository.findBySessionId(sessionId);
        List<GameSlide> allPresentedSlides = gameSlideRepository.findBySessionIdOrderBySlideIndexAsc(sessionId);

        List<UUID> playerIds = players.stream().map(Player::getPlayerId).collect(Collectors.toList());
        List<PlayerAnswer> allPlayerAnswers = playerIds.isEmpty() ? List.of() : playerAnswerRepository.findByPlayerIdIn(playerIds);

        int totalPresentedSlidesCount = allPresentedSlides.size();

        List<GameSlide> gradableQuestionSlides = allPresentedSlides.stream()
                .filter(this::isGradableQuestionSlide)
                .collect(Collectors.toList());
        List<UUID> gradableQuestionSlideIds = gradableQuestionSlides.stream()
                .map(GameSlide::getSlideId)
                .collect(Collectors.toList());

        long totalCorrectAnswersOnGradableQuestions = 0;
        long totalValidAnswersOnGradableQuestions = 0;
        long totalReactionTimeForCorrectGradable = 0;
        long countCorrectAnswersForReactionTimeAvg = 0;

        for (PlayerAnswer answer : allPlayerAnswers) {
            if (gradableQuestionSlideIds.contains(answer.getSlideId())) {
                if (!"TIMEOUT".equalsIgnoreCase(answer.getStatus()) && !"SKIPPED".equalsIgnoreCase(answer.getStatus())) {
                    totalValidAnswersOnGradableQuestions++;
                    if ("CORRECT".equalsIgnoreCase(answer.getStatus())) {
                        totalCorrectAnswersOnGradableQuestions++;
                        if (answer.getReactionTimeMs() != null) {
                            totalReactionTimeForCorrectGradable += answer.getReactionTimeMs();
                            countCorrectAnswersForReactionTimeAvg++;
                        }
                    }
                }
            }
        }

        double averageAccuracy = (totalValidAnswersOnGradableQuestions > 0) ?
                (double) totalCorrectAnswersOnGradableQuestions / totalValidAnswersOnGradableQuestions : 0.0;

        double averageTimeForCorrect = (countCorrectAnswersForReactionTimeAvg > 0) ?
                (double) totalReactionTimeForCorrectGradable / countCorrectAnswersForReactionTimeAvg : 0.0;

        long totalWrongAnswersOnGradableQuestions = 0;
        for(PlayerAnswer answer : allPlayerAnswers) {
            if(gradableQuestionSlideIds.contains(answer.getSlideId()) && "WRONG".equalsIgnoreCase(answer.getStatus())) {
                totalWrongAnswersOnGradableQuestions++;
            }
        }
        double averageIncorrectAnswerCount = !players.isEmpty() ?
                (double) totalWrongAnswersOnGradableQuestions / players.size() : 0.0;


        int scoredBlocksWithAnswersCount = (int) gradableQuestionSlideIds.stream()
                .filter(slideId -> allPlayerAnswers.stream().anyMatch(pa -> pa.getSlideId().equals(slideId)))
                .count();

        SessionSummaryDto.QuizInfo quizInfoDto = SessionSummaryDto.QuizInfo.builder()
                .quizId(quiz.getQuizId().toString())
                .title(quiz.getTitle())
                .creatorUserId(quiz.getCreatorId().toString())
                .creatorUsername(quizCreator != null ? quizCreator.getUsername() : "N/A")
                .build();

        return SessionSummaryDto.builder()
                .type(session.getGameType())
                .name(quiz.getTitle())
                .playerCount(session.getPlayerCount())
                .questionsCount(totalPresentedSlidesCount)
                .averageAccuracy(averageAccuracy)
                .time(DateTimeUtil.fromMillisToLong(session.getStartedAt()))
                .endTime(DateTimeUtil.fromMillisToLong(session.getEndedAt()))
                .username(host.getUsername())
                .hostId(host.getUserId().toString())
                .isScored(!gradableQuestionSlides.isEmpty())
                .hasCorrectness(!gradableQuestionSlides.isEmpty())
                .quizInfo(quizInfoDto)
                .scoredBlocksWithAnswersCount(scoredBlocksWithAnswersCount)
                .averageTime(averageTimeForCorrect)
                .averageIncorrectAnswerCount(averageIncorrectAnswerCount)
                .build();
    }

    private boolean isGradableQuestionSlide(GameSlide gameSlide) {
        if (gameSlide == null) return false;
        String slideTypeFromEntity = gameSlide.getSlideType();
        String slideTypeFromDistribution = parseQuestionTypeFromDistributionJson(gameSlide.getQuestionDistributionJson());
        String effectiveType = StringUtils.hasText(slideTypeFromDistribution) ? slideTypeFromDistribution : slideTypeFromEntity;
        if (effectiveType == null) return false;
        switch (effectiveType.toUpperCase()) {
            case "QUIZ":
            case "QUESTION_SLIDE":
            case "JUMBLE":
            case "OPEN_ENDED":
                return true;
            case "SURVEY": // Explicitly not gradable for accuracy
            case "CONTENT":
            case "CONTENT_SLIDE":
            case "LEADERBOARD":
                return false;
            default:
                log.trace("Unknown effective slide type for grading check: {}", effectiveType);
                return false;
        }
    }

    // (parseQuestionTypeFromDistributionJson, getPlayerReports, mapPlayerToReportItemDto remain the same)
    private String parseQuestionTypeFromDistributionJson(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            JsonNode rootNode = objectMapper.readTree(json);
            JsonNode typeNode = rootNode.path("type"); // Kahoot uses 'type', our QuestionDTO uses 'questionType'
            if (typeNode.isMissingNode() || !typeNode.isTextual()) { // Check our QuestionDTO structure if different
                typeNode = rootNode.path("questionType");
            }
            return typeNode.isTextual() ? typeNode.asText() : null;
        } catch (JsonProcessingException e) {
            log.warn("Could not parse question type from questionDistributionJson: {}", e.getMessage());
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PlayerReportItemDto> getPlayerReports(UUID sessionId, Pageable pageable) {
        log.info("Fetching player reports for session ID: {} with page request: {}", sessionId, pageable);

        if (!gameSessionRepository.existsById(sessionId)) {
            throw new ResourceNotFoundException("GameSession", "id", sessionId);
        }

        Page<Player> playerPage = playerRepository.findBySessionId(sessionId, pageable);

        List<PlayerReportItemDto> playerReportItems = playerPage.getContent().stream()
                .map(this::mapPlayerToReportItemDto)
                .collect(Collectors.toList());

        return new PageImpl<>(playerReportItems, pageable, playerPage.getTotalElements());
    }

    private PlayerReportItemDto mapPlayerToReportItemDto(Player player) {
        double playerAverageAccuracy = 0.0;
        // For player-specific accuracy, we'd need their answers to gradable questions.
        // The Player entity currently stores total correctAnswers and answerCount across ALL questions they answered.
        // This might be okay for an MVP player summary, or needs recalculation based on gradable questions if stricter.
        // For now, using existing Player fields:
        if (player.getAnswerCount() != null && player.getAnswerCount() > 0 && player.getCorrectAnswers() != null) {
            // To be more precise, this denominator should be answers to gradable questions only.
            // This might require fetching player's answers and filtering by gradable slides.
            // For MVP, this simpler calculation might suffice.
            playerAverageAccuracy = (double) player.getCorrectAnswers() / player.getAnswerCount();
        }


        double playerAveragePoints = 0.0;
        if (player.getAnswerCount() != null && player.getAnswerCount() > 0 && player.getTotalScore() != null) {
            playerAveragePoints = (double) player.getTotalScore() / player.getAnswerCount();
        }

        return PlayerReportItemDto.builder()
                .playerId(player.getPlayerId().toString())
                .clientId(player.getClientId())
                .nickname(player.getNickname())
                .rank(player.getRank())
                .answerCount(player.getAnswerCount())
                .unansweredCount(player.getUnansweredCount())
                .correctAnswers(player.getCorrectAnswers())
                .averageAccuracy(playerAverageAccuracy)
                .averagePoints(playerAveragePoints)
                .totalPoints(player.getTotalScore())
                .totalTime(player.getTotalTime())
                .averageTime(player.getAverageTime())
                .streakCount(player.getStreakCount())
                .build();
    }


    @Override
    @Transactional(readOnly = true)
    public Page<QuestionReportItemDto> getQuestionReports(UUID sessionId, Pageable pageable) {
        log.info("Fetching question reports for session ID: {} with page request: {}", sessionId, pageable);

        if (!gameSessionRepository.existsById(sessionId)) {
            throw new ResourceNotFoundException("GameSession", "id", sessionId);
        }

        // Fetch ALL GameSlides for the session first, then paginate the DTO results.
        // This is because Pageable applies to the root entity of the query.
        // We need all slides to correctly determine which ones are questions for the report.
        List<GameSlide> allSessionSlides = gameSlideRepository.findBySessionIdOrderBySlideIndexAsc(sessionId);

        List<QuestionReportItemDto> questionReportItems = allSessionSlides.stream()
                // We still want to report on all slides (including content) as per your request,
                // but statistics like accuracy will only apply to gradable ones.
                .map(slide -> {
                    List<PlayerAnswer> answersForThisSlide = playerAnswerRepository.findBySlideId(slide.getSlideId());
                    return mapGameSlideToQuestionReportItemDto(slide, answersForThisSlide);
                })
                .collect(Collectors.toList());

        // Manual pagination on the DTO list
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), questionReportItems.size());
        List<QuestionReportItemDto> pageContent = (start <= end && start < questionReportItems.size()) ? questionReportItems.subList(start, end) : Collections.emptyList();

        return new PageImpl<>(pageContent, pageable, questionReportItems.size());
    }

    private QuestionReportItemDto mapGameSlideToQuestionReportItemDto(GameSlide slide, List<PlayerAnswer> answersForThisSlide) {
        QuestionDTO originalQuestionData = null;
        if (StringUtils.hasText(slide.getQuestionDistributionJson())) {
            try {
                originalQuestionData = objectMapper.readValue(slide.getQuestionDistributionJson(), QuestionDTO.class);
            } catch (JsonProcessingException e) {
                log.error("Failed to parse questionDistributionJson for slideId {}: {}", slide.getSlideId(), e.getMessage());
            }
        }

        String title = originalQuestionData != null && originalQuestionData.getTitle() != null ? originalQuestionData.getTitle() : "N/A (Content Slide or Missing Title)";
        String effectiveSlideTypeForDisplay = parseQuestionTypeFromDistributionJson(slide.getQuestionDistributionJson());
        if (!StringUtils.hasText(effectiveSlideTypeForDisplay)) {
            effectiveSlideTypeForDisplay = slide.getSlideType();
        }

        List<ChoiceDTO> originalChoices = originalQuestionData != null && originalQuestionData.getChoices() != null ? originalQuestionData.getChoices() : Collections.emptyList();
        String imageUrl = originalQuestionData != null ? originalQuestionData.getImage() : null;
        com.vuiquiz.quizwebsocket.dto.VideoDetailDTO videoDetail = originalQuestionData != null ? originalQuestionData.getVideo() : null;

        long totalAnswersSubmitted = answersForThisSlide.size();
        long totalAnsweredControllers = answersForThisSlide.stream().map(PlayerAnswer::getPlayerId).distinct().count();

        Double questionAccuracy = null;
        Double averageTimeVal = null;

        if (isGradableQuestionSlide(slide)) {
            long correctAnswersCount = answersForThisSlide.stream().filter(a -> "CORRECT".equalsIgnoreCase(a.getStatus())).count();
            long validAnswersForAccuracy = answersForThisSlide.stream()
                    .filter(a -> !"TIMEOUT".equalsIgnoreCase(a.getStatus()) && !"SKIPPED".equalsIgnoreCase(a.getStatus()))
                    .count();
            questionAccuracy = (validAnswersForAccuracy > 0) ? (double) correctAnswersCount / validAnswersForAccuracy : 0.0;

            OptionalDouble avgTimeOpt = answersForThisSlide.stream()
                    .filter(a -> a.getReactionTimeMs() != null && (!"TIMEOUT".equalsIgnoreCase(a.getStatus()) && !"SKIPPED".equalsIgnoreCase(a.getStatus())) )
                    .mapToInt(PlayerAnswer::getReactionTimeMs)
                    .average();
            if (avgTimeOpt.isPresent()) {
                averageTimeVal = avgTimeOpt.getAsDouble();
            } else if (validAnswersForAccuracy > 0) {
                averageTimeVal = 0.0;
            }
        }

        List<AnswerDistributionDto> answerDistributionList = new ArrayList<>();
        // Populate distribution for gradable questions OR survey questions with choices
        boolean shouldPopulateDistribution = (isGradableQuestionSlide(slide) || "SURVEY".equalsIgnoreCase(effectiveSlideTypeForDisplay)) && !CollectionUtils.isEmpty(originalChoices);

        if (shouldPopulateDistribution) {
            for (int i = 0; i < originalChoices.size(); i++) {
                final int choiceIdx = i;
                ChoiceDTO choiceDto = originalChoices.get(i);
                long countForThisChoice = answersForThisSlide.stream()
                        .filter(pa -> playerChoseOption(pa.getChoice(), choiceIdx, objectMapper, slide.getSlideId()))
                        .count();

                String choiceStatus = "SURVEY_OPTION"; // Default for survey
                if (isGradableQuestionSlide(slide)) { // Override for gradable
                    choiceStatus = choiceDto.getCorrect() != null && choiceDto.getCorrect() ? "CORRECT" : "WRONG";
                }

                answerDistributionList.add(AnswerDistributionDto.builder()
                        .choiceIndex(choiceIdx)
                        .answerText(choiceDto.getAnswer())
                        .status(choiceStatus)
                        .count((int) countForThisChoice)
                        .build());
            }
        }
        // Add TIMEOUT to distribution only for gradable/survey questions that expect an answer
        if(isGradableQuestionSlide(slide) || "SURVEY".equalsIgnoreCase(effectiveSlideTypeForDisplay)){
            long timeoutCount = answersForThisSlide.stream().filter(a -> "TIMEOUT".equalsIgnoreCase(a.getStatus())).count();
            if (timeoutCount > 0) {
                answerDistributionList.add(AnswerDistributionDto.builder()
                        .choiceIndex(-3)
                        .answerText("Timeout")
                        .status("TIMEOUT")
                        .count((int) timeoutCount)
                        .build());
            }
        }

        return QuestionReportItemDto.builder()
                .slideIndex(slide.getSlideIndex())
                .title(title)
                .type(effectiveSlideTypeForDisplay)
                .choices(originalChoices)
                .imageUrl(imageUrl)
                .video(videoDetail)
                .totalAnswers((int) totalAnswersSubmitted)
                .totalAnsweredControllers((int) totalAnsweredControllers)
                .averageAccuracy(questionAccuracy)
                .averageTime(averageTimeVal)
                .answersDistribution(answerDistributionList)
                .build();
    }

    // Helper for answer distribution logic (playerChoseOption) remains the same
    private boolean playerChoseOption(String playerAnswerChoiceJson, int targetChoiceIndex, ObjectMapper mapper, UUID slideIdForLogging) {
        if (!StringUtils.hasText(playerAnswerChoiceJson)) return false;
        try {
            JsonNode choiceNode = mapper.readTree(playerAnswerChoiceJson);
            if (choiceNode.isNumber()) {
                return choiceNode.asInt() == targetChoiceIndex;
            } else if (choiceNode.isArray()) {
                for (JsonNode cn : choiceNode) {
                    if (cn.asInt() == targetChoiceIndex) return true;
                }
            } else if (choiceNode.isTextual()) {
                if (playerAnswerChoiceJson.matches("\\d+")) {
                    return Integer.parseInt(playerAnswerChoiceJson) == targetChoiceIndex;
                }
            }
        } catch (JsonProcessingException | NumberFormatException e) {
            log.trace("Could not parse choice string '{}' for answer distribution on slide {}", playerAnswerChoiceJson, slideIdForLogging);
        }
        return false;
    }


    @Override
    @Transactional(readOnly = true)
    public Page<PlayerAnswerReportItemDto> getPlayerAnswersReport(UUID sessionId, UUID playerId, Pageable pageable) {
        log.info("Fetching answers report for player ID: {} in session ID: {} with page request: {}", playerId, sessionId, pageable);

        if (!gameSessionRepository.existsById(sessionId)) {
            throw new ResourceNotFoundException("GameSession", "id", sessionId);
        }
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Player", "id", playerId));
        if (!player.getSessionId().equals(sessionId)) {
            log.warn("Player {} does not belong to session {}. Access denied or data mismatch.", playerId, sessionId);
            throw new IllegalArgumentException("Player " + playerId + " does not belong to session " + sessionId);
        }

        List<PlayerAnswer> playerAnswers = playerAnswerRepository.findByPlayerId(playerId);

        // Map to DTO and include slideIndex for sorting before manual pagination
        List<Map.Entry<GameSlide, PlayerAnswer>> enrichedAnswers = playerAnswers.stream()
                .map(answer -> {
                    Optional<GameSlide> slideOpt = gameSlideRepository.findById(answer.getSlideId());
                    // Filter out answers for slides not belonging to the requested session (additional safety)
                    return slideOpt.filter(s -> s.getSessionId().equals(sessionId))
                            .map(s -> new AbstractMap.SimpleEntry<>(s, answer));
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        // Sort by slide index
        enrichedAnswers.sort(Comparator.comparingInt(entry -> entry.getKey().getSlideIndex()));

        List<PlayerAnswerReportItemDto> reportItems = enrichedAnswers.stream()
                .map(entry -> mapPlayerAnswerToReportItemDto(entry.getValue(), entry.getKey()))
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), reportItems.size());
        List<PlayerAnswerReportItemDto> pageContent = (start <= end && start < reportItems.size()) ? reportItems.subList(start, end) : Collections.emptyList();

        return new PageImpl<>(pageContent, pageable, reportItems.size());
    }

    private PlayerAnswerReportItemDto mapPlayerAnswerToReportItemDto(PlayerAnswer answer, GameSlide slide) {
        QuestionDTO originalQuestionData = null;
        if (StringUtils.hasText(slide.getQuestionDistributionJson())) {
            try {
                originalQuestionData = objectMapper.readValue(slide.getQuestionDistributionJson(), QuestionDTO.class);
            } catch (JsonProcessingException e) {
                log.error("Failed to parse questionDistributionJson for slideId {}: {}", slide.getSlideId(), e.getMessage());
            }
        }
        List<ChoiceDTO> originalChoices = (originalQuestionData != null && originalQuestionData.getChoices() != null) ?
                originalQuestionData.getChoices() : Collections.emptyList();
        String blockTitle = (originalQuestionData != null && originalQuestionData.getTitle() != null) ?
                originalQuestionData.getTitle() : "N/A";

        String effectiveBlockType = parseQuestionTypeFromDistributionJson(slide.getQuestionDistributionJson());
        if(!StringUtils.hasText(effectiveBlockType)) {
            effectiveBlockType = slide.getSlideType();
        }


        String displayText = "N/A";
        if (answer.getText() != null) { // Prioritize text answer if present (for open_ended)
            displayText = answer.getText();
        } else if (StringUtils.hasText(answer.getChoice())) {
            try {
                JsonNode choiceNode = objectMapper.readTree(answer.getChoice());
                if (choiceNode.isNumber()) {
                    int choiceIdx = choiceNode.asInt();
                    if (choiceIdx >= 0 && choiceIdx < originalChoices.size()) {
                        displayText = originalChoices.get(choiceIdx).getAnswer();
                    } else {
                        displayText = "Choice index out of bounds: " + choiceIdx;
                    }
                } else if (choiceNode.isArray() && !originalChoices.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (JsonNode cn : choiceNode) {
                        int idx = cn.asInt();
                        if (idx >= 0 && idx < originalChoices.size()) {
                            if (sb.length() > 0) sb.append(", ");
                            sb.append(originalChoices.get(idx).getAnswer());
                        }
                    }
                    displayText = sb.length() > 0 ? sb.toString() : "Selected choices (Array)";
                } else if (choiceNode.isTextual() && answer.getChoice().matches("\\d+")){
                    int choiceIdx = Integer.parseInt(answer.getChoice());
                    if (choiceIdx >= 0 && choiceIdx < originalChoices.size()) {
                        displayText = originalChoices.get(choiceIdx).getAnswer();
                    } else {
                        displayText = "Choice index out of bounds: " + choiceIdx;
                    }
                } else { // Fallback for non-numeric, non-array textual choice
                    displayText = answer.getChoice();
                }
            } catch (JsonProcessingException | NumberFormatException e) {
                log.warn("Could not parse player's choice '{}' to determine display text for answerId {}: {}", answer.getChoice(), answer.getAnswerId(), e.getMessage());
                displayText = answer.getChoice();
            }
        } else if ("TIMEOUT".equalsIgnoreCase(answer.getStatus())) {
            displayText = "Timeout";
        } else if ("SKIPPED".equalsIgnoreCase(answer.getStatus())) {
            displayText = "Skipped";
        }


        PlayerAnswerReportItemDto.AnswerDetails answerDetails = PlayerAnswerReportItemDto.AnswerDetails.builder()
                .type(effectiveBlockType + "_answer")
                .choice(answer.getChoice())
                .text(answer.getText())
                .reactionTime(answer.getReactionTimeMs())
                .points(answer.getFinalPoints())
                .blockIndex(slide.getSlideIndex())
                .blockType(effectiveBlockType)
                .status(answer.getStatus())
                .build();

        PlayerAnswerReportItemDto.QuestionContextData questionContext = PlayerAnswerReportItemDto.QuestionContextData.builder()
                .displayText(displayText)
                .blockTitle(blockTitle)
                .blockIndex(slide.getSlideIndex())
                .blockChoices(originalChoices)
                .build();

        return PlayerAnswerReportItemDto.builder()
                .answerDetails(answerDetails)
                .questionContextData(questionContext)
                .build();
    }


    // --- New Method for Reporting Phase 5 ---
    @Override
    @Transactional(readOnly = true)
    public Page<UserSessionHistoryItemDto> getCurrentUserSessions(Pageable pageable) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
            log.warn("Attempt to get current user sessions without proper authentication.");
            // Depending on how strict you want to be, you could throw an exception
            // or return an empty page. Spring Security should ideally prevent this.
            throw new UnauthorizedException("User must be authenticated to access their session history.");
        }
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        UUID currentUserId = userDetails.getId();

        log.info("Fetching all participated/hosted sessions for authenticated user ID: {} with page request: {}", currentUserId, pageable);

        // 1. Fetch sessions hosted by the user
        List<GameSession> hostedSessions = gameSessionRepository.findByHostId(currentUserId);

        // 2. Fetch sessions participated in by the user
        List<Player> playerRecords = playerRepository.findByUserId(currentUserId);
        List<UUID> participatedSessionIds = playerRecords.stream()
                .map(Player::getSessionId)
                .distinct()
                .collect(Collectors.toList());

        List<GameSession> participatedSessions = participatedSessionIds.isEmpty() ?
                Collections.emptyList() :
                gameSessionRepository.findAllById(participatedSessionIds);

        // 3. Combine and Deduplicate
        Map<UUID, GameSession> distinctSessionsMap = new HashMap<>();
        hostedSessions.forEach(session -> distinctSessionsMap.put(session.getSessionId(), session));
        participatedSessions.forEach(session -> distinctSessionsMap.putIfAbsent(session.getSessionId(), session));

        List<GameSession> allRelevantSessions = new ArrayList<>(distinctSessionsMap.values());

        // 4. Sort the combined list based on Pageable
        // Manual sort implementation based on common properties
        Sort sort = pageable.getSort();
        if (sort.isSorted()) {
            for (Sort.Order order : sort) {
                Comparator<GameSession> comparator = null;
                switch (order.getProperty().toLowerCase()) {
                    case "time": // Maps to startedAt
                    case "startedat":
                        comparator = Comparator.comparing(GameSession::getStartedAt, Comparator.nullsLast(OffsetDateTime::compareTo));
                        break;
                    case "endtime":
                        comparator = Comparator.comparing(GameSession::getEndedAt, Comparator.nullsLast(OffsetDateTime::compareTo));
                        break;
                    case "playercount":
                        comparator = Comparator.comparing(GameSession::getPlayerCount, Comparator.nullsLast(Integer::compareTo));
                        break;
                    // Add more sortable GameSession properties here if needed
                }
                if (comparator != null) {
                    if (order.isDescending()) {
                        comparator = comparator.reversed();
                    }
                    allRelevantSessions.sort(comparator);
                } else {
                    log.warn("Unsupported sort property for UserSessionHistory: {}", order.getProperty());
                }
            }
        } else {
            // Default sort if no sort provided in Pageable: by startedAt descending
            allRelevantSessions.sort(Comparator.comparing(GameSession::getStartedAt, Comparator.nullsLast(OffsetDateTime::compareTo)).reversed());
        }


        // 5. Manual Pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allRelevantSessions.size());
        List<GameSession> pagedSessions = (start <= end && start < allRelevantSessions.size()) ? allRelevantSessions.subList(start, end) : Collections.emptyList();

        // 6. Populate DTOs (Batch fetch related entities for the current page)
        Set<UUID> quizIdsForPage = pagedSessions.stream().map(GameSession::getQuizId).collect(Collectors.toSet());
        Map<UUID, Quiz> quizMap = quizIdsForPage.isEmpty() ? Collections.emptyMap() :
                quizRepository.findAllById(quizIdsForPage).stream().collect(Collectors.toMap(Quiz::getQuizId, Function.identity()));

        Set<UUID> hostIdsForPage = pagedSessions.stream().map(GameSession::getHostId).collect(Collectors.toSet());
        Map<UUID, UserAccount> hostUserMap = hostIdsForPage.isEmpty() ? Collections.emptyMap() :
                userAccountRepository.findAllById(hostIdsForPage).stream().collect(Collectors.toMap(UserAccount::getUserId, Function.identity()));

        List<UserSessionHistoryItemDto> dtos = pagedSessions.stream().map(session -> {
            Quiz quiz = quizMap.get(session.getQuizId());
            UserAccount sessionHost = hostUserMap.get(session.getHostId());
            String roleInSession = session.getHostId().equals(currentUserId) ? "HOST" : "PLAYER";

            return UserSessionHistoryItemDto.builder()
                    .sessionId(session.getSessionId().toString())
                    .name(quiz != null ? quiz.getTitle() : "N/A")
                    .time(DateTimeUtil.fromMillisToLong(session.getStartedAt()))
                    .endTime(DateTimeUtil.fromMillisToLong(session.getEndedAt()))
                    .type(session.getGameType())
                    .playerCount(session.getPlayerCount())
                    .roleInSession(roleInSession)
                    .sessionHostUserId(session.getHostId().toString())
                    .sessionHostUsername(sessionHost != null ? sessionHost.getUsername() : "N/A")
                    .quizId(session.getQuizId().toString())
                    .build();
        }).collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, allRelevantSessions.size());
    }
}