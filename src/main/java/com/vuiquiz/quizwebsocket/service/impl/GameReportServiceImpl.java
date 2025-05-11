// src/main/java/com/vuiquiz/quizwebsocket/service/impl/GameReportServiceImpl.java
package com.vuiquiz.quizwebsocket.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vuiquiz.quizwebsocket.dto.ChoiceDTO;
import com.vuiquiz.quizwebsocket.dto.QuestionDTO; // For parsing questionDistributionJson
import com.vuiquiz.quizwebsocket.dto.report.*;
import com.vuiquiz.quizwebsocket.exception.ResourceNotFoundException;
import com.vuiquiz.quizwebsocket.model.*;
import com.vuiquiz.quizwebsocket.repository.*;
import com.vuiquiz.quizwebsocket.service.GameReportService;
import com.vuiquiz.quizwebsocket.utils.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
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

    // Helper method to determine if a slide type is a question meant for grading/answering
    private boolean isGradableQuestionSlide(GameSlide gameSlide) {
        if (gameSlide == null) return false;
        String slideTypeFromEntity = gameSlide.getSlideType(); // e.g., QUESTION_SLIDE, CONTENT_SLIDE
        String slideTypeFromDistribution = parseQuestionTypeFromDistributionJson(gameSlide.getQuestionDistributionJson()); // e.g., quiz, survey, open_ended

        // Prioritize type from distribution if available, as it's more specific from quiz design
        String effectiveType = StringUtils.hasText(slideTypeFromDistribution) ? slideTypeFromDistribution : slideTypeFromEntity;

        if (effectiveType == null) return false;

        switch (effectiveType.toUpperCase()) {
            case "QUIZ":
            case "QUESTION_SLIDE": // Generic type for questions
            case "JUMBLE":
            case "OPEN_ENDED":
                return true;
            case "SURVEY":
            case "CONTENT":
            case "CONTENT_SLIDE":
            case "LEADERBOARD":
                return false;
            default:
                log.warn("Unknown effective slide type for grading check: {}", effectiveType);
                return false; // Default to not gradable if unknown
        }
    }


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
        UserAccount quizCreator = quiz.getCreatorId() != null ? userAccountRepository.findById(quiz.getCreatorId()).orElse(null) : null;

        List<Player> players = playerRepository.findBySessionId(sessionId);
        List<GameSlide> allPresentedSlides = gameSlideRepository.findBySessionIdOrderBySlideIndexAsc(sessionId);

        List<UUID> playerIds = players.stream().map(Player::getPlayerId).collect(Collectors.toList());
        List<PlayerAnswer> allPlayerAnswers = playerIds.isEmpty() ? List.of() : playerAnswerRepository.findByPlayerIdIn(playerIds);

        // 1. questionsCount: Total slides presented (including content, etc., as per your request)
        int totalPresentedSlidesCount = allPresentedSlides.size();

        // Identify gradable question slides and their IDs
        List<GameSlide> gradableQuestionSlides = allPresentedSlides.stream()
                .filter(this::isGradableQuestionSlide)
                .collect(Collectors.toList());
        List<UUID> gradableQuestionSlideIds = gradableQuestionSlides.stream()
                .map(GameSlide::getSlideId)
                .collect(Collectors.toList());

        long totalCorrectAnswersOnGradableQuestions = 0;
        long totalValidAnswersOnGradableQuestions = 0; // Answers that were not TIMEOUT or SKIPPED
        long totalReactionTimeForCorrectGradable = 0;
        long countCorrectAnswersForReactionTimeAvg = 0;

        for (PlayerAnswer answer : allPlayerAnswers) {
            if (gradableQuestionSlideIds.contains(answer.getSlideId())) {
                // Only consider answers that were actually submitted (not timeout/skipped) for accuracy denominator
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

        // 2. averageAccuracy: Based on gradable questions only
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

        // 3. scoredBlocksWithAnswersCount: Gradable question slides that received at least one answer
        // (Here, "answer" means any PlayerAnswer record, could include TIMEOUT if those are stored)
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
                .questionsCount(totalPresentedSlidesCount) // Adjusted: count all presented slides
                .averageAccuracy(averageAccuracy) // Adjusted: based on gradable questions
                .time(DateTimeUtil.fromMillisToLong(session.getStartedAt()))
                .endTime(DateTimeUtil.fromMillisToLong(session.getEndedAt()))
                .username(host.getUsername())
                .hostId(host.getUserId().toString())
                .isScored(!gradableQuestionSlides.isEmpty()) // True if there's at least one gradable question
                .hasCorrectness(!gradableQuestionSlides.isEmpty()) // Similar logic for MVP
                .quizInfo(quizInfoDto)
                .scoredBlocksWithAnswersCount(scoredBlocksWithAnswersCount) // Based on gradable questions
                .averageTime(averageTimeForCorrect) // Based on correct answers to gradable questions
                .averageIncorrectAnswerCount(averageIncorrectAnswerCount) // Based on wrong answers to gradable questions
                .build();
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
        // Use effective type for display
        String effectiveSlideTypeForDisplay = parseQuestionTypeFromDistributionJson(slide.getQuestionDistributionJson());
        if (!StringUtils.hasText(effectiveSlideTypeForDisplay)) {
            effectiveSlideTypeForDisplay = slide.getSlideType();
        }

        List<ChoiceDTO> originalChoices = originalQuestionData != null && originalQuestionData.getChoices() != null ? originalQuestionData.getChoices() : Collections.emptyList();
        String imageUrl = originalQuestionData != null ? originalQuestionData.getImage() : null;
        com.vuiquiz.quizwebsocket.dto.VideoDetailDTO videoDetail = originalQuestionData != null ? originalQuestionData.getVideo() : null;

        long totalAnswersSubmitted = answersForThisSlide.size(); // All answer attempts including timeouts
        long totalAnsweredControllers = answersForThisSlide.stream().map(PlayerAnswer::getPlayerId).distinct().count();

        Double questionAccuracy = null; // Null if not a gradable question
        Double averageTime = null;      // Null if not a gradable question or no answers

        if (isGradableQuestionSlide(slide)) {
            long correctAnswersCount = answersForThisSlide.stream().filter(a -> "CORRECT".equalsIgnoreCase(a.getStatus())).count();
            long validAnswersForAccuracy = answersForThisSlide.stream()
                    .filter(a -> !"TIMEOUT".equalsIgnoreCase(a.getStatus()) && !"SKIPPED".equalsIgnoreCase(a.getStatus()))
                    .count();
            questionAccuracy = (validAnswersForAccuracy > 0) ? (double) correctAnswersCount / validAnswersForAccuracy : 0.0;

            averageTime = answersForThisSlide.stream()
                    .filter(a -> a.getReactionTimeMs() != null && (!"TIMEOUT".equalsIgnoreCase(a.getStatus()) && !"SKIPPED".equalsIgnoreCase(a.getStatus())) )
                    .mapToInt(PlayerAnswer::getReactionTimeMs)
                    .average().orElse(0.0);
        }


        List<AnswerDistributionDto> answerDistributionList = new ArrayList<>();
        if (isGradableQuestionSlide(slide) && !CollectionUtils.isEmpty(originalChoices)) { // Only show distribution for gradable with choices
            for (int i = 0; i < originalChoices.size(); i++) {
                final int choiceIdx = i;
                ChoiceDTO choiceDto = originalChoices.get(i);
                long countForThisChoice = answersForThisSlide.stream()
                        .filter(pa -> playerChoseOption(pa.getChoice(), choiceIdx, objectMapper, slide.getSlideId()))
                        .count();

                answerDistributionList.add(AnswerDistributionDto.builder()
                        .choiceIndex(choiceIdx)
                        .answerText(choiceDto.getAnswer())
                        .status(choiceDto.getCorrect() != null && choiceDto.getCorrect() ? "CORRECT" : "WRONG")
                        .count((int) countForThisChoice)
                        .build());
            }
        }
        // Add TIMEOUT to distribution only for gradable questions
        if(isGradableQuestionSlide(slide)){
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
                .type(effectiveSlideTypeForDisplay) // Display effective type
                .choices(originalChoices)
                .imageUrl(imageUrl)
                .video(videoDetail)
                .totalAnswers((int) totalAnswersSubmitted) // Total attempts on this slide
                .totalAnsweredControllers((int) totalAnsweredControllers) // Distinct players who interacted
                .averageAccuracy(questionAccuracy) // Null if not gradable
                .averageTime(averageTime) // Null if not gradable or no valid answers
                .answersDistribution(answerDistributionList) // Empty if not gradable with choices
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
            log.warn("Player {} does not belong to session {}.", playerId, sessionId);
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
}