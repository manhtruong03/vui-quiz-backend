package com.vuiquiz.quizwebsocket.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vuiquiz.quizwebsocket.dto.ChoiceDTO;
import com.vuiquiz.quizwebsocket.dto.QuestionDTO;
import com.vuiquiz.quizwebsocket.dto.QuizDTO;
import com.vuiquiz.quizwebsocket.dto.VideoDetailDTO;
import com.vuiquiz.quizwebsocket.exception.FileStorageException;
import com.vuiquiz.quizwebsocket.exception.ResourceNotFoundException;
import com.vuiquiz.quizwebsocket.exception.StorageQuotaExceededException;
import com.vuiquiz.quizwebsocket.model.*;
import com.vuiquiz.quizwebsocket.repository.*;
import com.vuiquiz.quizwebsocket.security.services.UserDetailsImpl;
import com.vuiquiz.quizwebsocket.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class QuizServiceImpl implements QuizService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final ImageStorageService imageStorageService;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;
    private final UserAccountRepository userAccountRepository;
    private final ImageStorageRepository imageStorageRepository;
    private final TagService tagService; // Inject TagService
    private final QuizTagService quizTagService; // Inject QuizTagService
    private final QuizTagRepository quizTagRepository; // Inject Repositories for batch fetching
    private final TagRepository tagRepository;         // Inject Repositories for batch fetching
    private final UserAccountService userAccountService;


    @Autowired
    public QuizServiceImpl(QuizRepository quizRepository,
                           QuestionRepository questionRepository,
                           ImageStorageService imageStorageService, FileStorageService fileStorageService,
                           ObjectMapper objectMapper,
                           UserAccountRepository userAccountRepository,
                           ImageStorageRepository imageStorageRepository,
                           TagService tagService,               // Add to constructor
                           QuizTagService quizTagService,         // Add to constructor
                           QuizTagRepository quizTagRepository,   // Add to constructor
                           TagRepository tagRepository, UserAccountService userAccountService) {           // Add to constructor
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.imageStorageService = imageStorageService;
        this.fileStorageService = fileStorageService;
        this.objectMapper = objectMapper;
        this.userAccountRepository = userAccountRepository;
        this.imageStorageRepository = imageStorageRepository;
        this.tagService = tagService;                 // Assign
        this.quizTagService = quizTagService;         // Assign
        this.quizTagRepository = quizTagRepository;   // Assign
        this.tagRepository = tagRepository;           // Assign
        this.userAccountService = userAccountService;
    }

    @Override
    @Transactional
    public QuizDTO createQuiz(QuizDTO quizDto, UUID creatorId, Map<String, MultipartFile> imageFiles) {
        // Ensure imageFiles map is not null if used, though it can be empty
        if (imageFiles == null) {
            imageFiles = Collections.emptyMap();
        }

        // 0. Determine total upload size and check quota
        long totalUploadSize = 0;
        MultipartFile coverImageFile = null;

        // Check for cover image
        if (quizDto.getCoverImageUploadKey() != null) {
            coverImageFile = imageFiles.get(quizDto.getCoverImageUploadKey());
            if (coverImageFile != null && !coverImageFile.isEmpty()) {
                totalUploadSize += coverImageFile.getSize();
            } else {
                // If key was provided but no file, log or handle as an error/warning
                log.warn("CoverImageUploadKey '{}' was provided in quizData, but no corresponding file was found in imageFiles map for quiz title '{}'.", quizDto.getCoverImageUploadKey(), quizDto.getTitle());
                coverImageFile = null; // Ensure it's null if not found or empty
            }
        }

        // Collect question images and their sizes
        Map<String, MultipartFile> questionImagesMapForProcessing = new HashMap<>();
        if (quizDto.getQuestions() != null) {
            for (QuestionDTO qDto : quizDto.getQuestions()) {
                if (qDto.getQuestionImageUploadKey() != null) {
                    MultipartFile qFile = imageFiles.get(qDto.getQuestionImageUploadKey());
                    if (qFile != null && !qFile.isEmpty()) {
                        totalUploadSize += qFile.getSize();
                        questionImagesMapForProcessing.put(qDto.getQuestionImageUploadKey(), qFile);
                    } else {
                        log.warn("QuestionImageUploadKey '{}' was provided for question '{}' (quiz title '{}'), but no corresponding file was found in imageFiles map.", qDto.getQuestionImageUploadKey(), qDto.getTitle(), quizDto.getTitle());
                    }
                }
            }
        }

        if (totalUploadSize > 0) {
            if (!userAccountService.canUserUpload(creatorId, totalUploadSize)) {
                throw new StorageQuotaExceededException("Upload exceeds storage quota. Required: " + totalUploadSize + " bytes.");
            }
        }

        Quiz quiz = new Quiz();
        quiz.setCreatorId(creatorId);
        quiz.setTitle(quizDto.getTitle());
        quiz.setDescription(quizDto.getDescription());
        quiz.setVisibility(quizDto.getVisibility() != null ? quizDto.getVisibility() : 0);
        quiz.setStatus(StringUtils.hasText(quizDto.getStatus()) ? quizDto.getStatus() : "DRAFT");
        quiz.setQuizTypeInfo(quizDto.getQuizType()); // Assuming quizDto.getQuizType() maps to quizTypeInfo

        // 1. Handle Cover Image
        if (coverImageFile != null && !coverImageFile.isEmpty()) {
            try {
                String storedCoverFileName = fileStorageService.storeFile(coverImageFile);
                ImageStorage coverImageRecord = imageStorageService.createImageRecord(coverImageFile, storedCoverFileName, creatorId);
                quiz.setCoverImageId(coverImageRecord.getImageId());
            } catch (FileStorageException e) {
                log.error("Failed to store cover image for quiz title '{}': {}", quizDto.getTitle(), e.getMessage());
                // Depending on requirements, you might throw a specialized exception or wrap and rethrow
                throw new FileStorageException("Failed to store cover image: " + e.getMessage(), e);
            }
        }

        if (quizDto.getLobbyVideo() != null) {
            try {
                quiz.setLobbyVideoJson(objectMapper.writeValueAsString(quizDto.getLobbyVideo()));
            } catch (JsonProcessingException e) {
                log.error("Error serializing lobby video DTO for quiz title '{}': {}", quizDto.getTitle(), e.getMessage());
                quiz.setLobbyVideoJson(null); // Or handle error more gracefully
            }
        }

        Quiz savedQuiz = quizRepository.save(quiz);
        UUID persistedQuizId = savedQuiz.getQuizId();
        int questionCount = 0;

        if (!CollectionUtils.isEmpty(quizDto.getQuestions())) {
            List<Question> questionsToSave = new ArrayList<>();
            for (int i = 0; i < quizDto.getQuestions().size(); i++) {
                QuestionDTO questionDtoInternal = quizDto.getQuestions().get(i);
                if (questionDtoInternal.getTitle() == null || questionDtoInternal.getTitle().isBlank() ||
                        questionDtoInternal.getType() == null || questionDtoInternal.getType().isBlank()) {
                    log.warn("Skipping question at index {} due to missing title or type for quiz '{}'", i, quizDto.getTitle());
                    continue;
                }

                Question question = new Question();
                question.setQuizId(persistedQuizId);
                question.setQuestionType(questionDtoInternal.getType());
                question.setQuestionText(questionDtoInternal.getTitle());
                question.setDescriptionText(questionDtoInternal.getDescription());
                question.setTimeLimit(questionDtoInternal.getTime() != null ? questionDtoInternal.getTime() : 0);
                question.setPointsMultiplier(questionDtoInternal.getPointsMultiplier() != null ? questionDtoInternal.getPointsMultiplier() : 0);
                question.setPosition(questionDtoInternal.getPosition() != null ? questionDtoInternal.getPosition() : i);

                // 2. Handle Question Image using its upload key
                if (questionDtoInternal.getQuestionImageUploadKey() != null) {
                    MultipartFile questionImageFile = questionImagesMapForProcessing.get(questionDtoInternal.getQuestionImageUploadKey());
                    if (questionImageFile != null && !questionImageFile.isEmpty()) {
                        try {
                            String storedQuestionImageName = fileStorageService.storeFile(questionImageFile);
                            ImageStorage questionImageRecord = imageStorageService.createImageRecord(questionImageFile, storedQuestionImageName, creatorId);
                            question.setImageId(questionImageRecord.getImageId());
                        } catch (FileStorageException e) {
                            log.error("Failed to store image for question '{}' (quiz title '{}'): {}", questionDtoInternal.getTitle(), quizDto.getTitle(), e.getMessage());
                            // Depending on requirements, decide if this should halt creation or just skip the image
                            // For now, we'll let it skip the image if storage fails for a question image
                        }
                    }
                }

                if (questionDtoInternal.getVideo() != null) {
                    try {
                        question.setVideoContentJson(objectMapper.writeValueAsString(questionDtoInternal.getVideo()));
                    } catch (JsonProcessingException e) {
                        log.error("Error serializing question video DTO for question '{}': {}", questionDtoInternal.getTitle(), e.getMessage());
                    }
                }
                if (!CollectionUtils.isEmpty(questionDtoInternal.getChoices())) {
                    try {
                        question.setAnswerDataJson(objectMapper.writeValueAsString(questionDtoInternal.getChoices()));
                    } catch (JsonProcessingException e) {
                        log.error("Error serializing question choices DTO for question '{}': {}", questionDtoInternal.getTitle(), e.getMessage());
                        question.setAnswerDataJson("[]"); // Default to empty JSON array on error
                    }
                } else {
                    question.setAnswerDataJson("[]"); // Default to empty JSON array if no choices
                }

                questionsToSave.add(question);
                questionCount++;
            }
            if (!questionsToSave.isEmpty()) {
                questionRepository.saveAll(questionsToSave);
            }
        }

        // Handle Tags
        if (!CollectionUtils.isEmpty(quizDto.getTags())) {
            for (String tagName : quizDto.getTags()) {
                try {
                    Tag tag = tagService.findOrCreateTagByName(tagName);
                    quizTagService.addTagToQuiz(persistedQuizId, tag.getTagId());
                } catch (Exception e) { // Catch broader exception for tag processing
                    log.warn("Skipping tag '{}' for quiz '{}' (ID: {}) due to error: {}", tagName, quizDto.getTitle(), persistedQuizId, e.getMessage());
                }
            }
        }

        savedQuiz.setQuestionCount(questionCount);
        Quiz finalSavedQuiz = quizRepository.save(savedQuiz);

        // 3. Update user storage used AFTER all DB operations for quiz/questions are successful
        if (totalUploadSize > 0) {
            try {
                userAccountService.updateUserStorageUsed(creatorId, totalUploadSize);
            } catch (Exception e) {
                log.error("CRITICAL: Quiz {} created and images stored, but failed to update user {} storage by {}. Manual reconciliation needed. Error: {}",
                        finalSavedQuiz.getQuizId(), creatorId, totalUploadSize, e.getMessage());
                // Quiz creation succeeds, but storage update issue is logged.
            }
        }

        // Map to DTO for response.
        // Note: The 'cover' and question 'image' URLs in the response DTO will be resolved by the mappers.
        // The original DTO contained uploadKeys, but the response DTO should contain actual URLs.
        // The mapQuizEntityToDto and mapQuestionEntityToDto methods should handle resolving ImageStorage IDs to public URLs.
        return mapQuizEntityToDto(finalSavedQuiz,
                userAccountRepository.findById(creatorId).map(UserAccount::getUsername).orElse(null),
                true, // loadQuestions for the response of create
                getTagNamesForQuiz(finalSavedQuiz.getQuizId()),
                true // calculateTotalTimeLimit
        );
    }

    @Override
    @Transactional(readOnly = true)
    public QuizDTO getQuizDetailsById(UUID quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz", "id", quizId));

        String creatorUsername = userAccountRepository.findById(quiz.getCreatorId())
                .map(UserAccount::getUsername)
                .orElse(null);

        // Fetch Tags for this quiz
        List<String> tagNames = getTagNamesForQuiz(quizId);

        return mapQuizEntityToDto(quiz, creatorUsername, true, tagNames, true); // Pass tag names, load questions = true
    }

    // Helper to get tag names
    private List<String> getTagNamesForQuiz(UUID quizId) {
        List<QuizTag> quizTags = quizTagRepository.findByQuizId(quizId);
        if (quizTags.isEmpty()) {
            return Collections.emptyList();
        }
        Set<UUID> tagIds = quizTags.stream().map(QuizTag::getTagId).collect(Collectors.toSet());
        List<Tag> tags = tagRepository.findAllById(tagIds); // Make sure tagRepository is injected
        return tags.stream().map(Tag::getName).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuizDTO> getQuizzesByCurrentUser(Pageable pageable) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        UUID currentUserId = userDetails.getId();
        String currentUsername = userDetails.getUsername();

        Page<Quiz> quizPage = quizRepository.findByCreatorId(currentUserId, pageable);
        List<Quiz> quizzes = quizPage.getContent();

        if (quizzes.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, quizPage.getTotalElements());
        }

        // Efficiently fetch related data
        Set<UUID> coverImageIds = quizzes.stream().map(Quiz::getCoverImageId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<UUID, String> coverImageUrlMap = imageStorageRepository.findAllById(coverImageIds).stream()
                .collect(Collectors.toMap(ImageStorage::getImageId, ImageStorage::getFilePath));

        List<UUID> quizIds = quizzes.stream().map(Quiz::getQuizId).collect(Collectors.toList());
        List<QuizTag> allQuizTags = quizTagRepository.findByQuizIdIn(quizIds);
        Set<UUID> allTagIds = allQuizTags.stream().map(QuizTag::getTagId).collect(Collectors.toSet());
        Map<UUID, String> tagNameMap = tagRepository.findAllById(allTagIds).stream()
                .collect(Collectors.toMap(Tag::getTagId, Tag::getName));
        Map<UUID, List<String>> tagsByQuizIdMap = allQuizTags.stream()
                .collect(Collectors.groupingBy(QuizTag::getQuizId,
                        Collectors.mapping(qt -> tagNameMap.get(qt.getTagId()), Collectors.toList())));

        // --- TWEAK 2: Fetch questions for each quiz to calculate total time limit ---
        // This can lead to N+1 if not handled carefully. Fetching all questions for all quizzes on the page:
        Map<UUID, List<Question>> questionsByQuizIdMap = new HashMap<>();
        if (!quizIds.isEmpty()) {
            List<Question> allQuestionsForPageQuizzes = questionRepository.findByQuizIdIn(quizIds);
            questionsByQuizIdMap = allQuestionsForPageQuizzes.stream().collect(Collectors.groupingBy(Question::getQuizId));
        }
        // --- END TWEAK 2 PREPARATION ---

        Map<UUID, List<Question>> finalQuestionsMap = questionsByQuizIdMap; // Effective final for lambda

        // 4. Map Quiz entities to QuizDTOs
        List<QuizDTO> quizDTOs = quizzes.stream()
                .map(quiz -> {
                    String coverFilePath = quiz.getCoverImageId() != null ? coverImageUrlMap.get(quiz.getCoverImageId()) : null;
                    String fullCoverUrl = null;
                    if (StringUtils.hasText(coverFilePath)) { // Check if filePath is not null or empty
                        fullCoverUrl = imageStorageService.getPublicUrl(coverFilePath); // Construct full URL
                    }
                    List<String> tagNames = tagsByQuizIdMap.getOrDefault(quiz.getQuizId(), Collections.emptyList());

                    // --- TWEAK 2: Calculate total time limit ---
                    List<Question> quizQuestions = finalQuestionsMap.getOrDefault(quiz.getQuizId(), Collections.emptyList());
                    int totalTimeLimitMs = quizQuestions.stream()
                            .filter(q -> q.getTimeLimit() != null)
                            .mapToInt(Question::getTimeLimit)
                            .sum();
                    // --- END TWEAK 2 CALCULATION ---

                    return mapQuizEntityToListDTO(quiz, currentUsername, fullCoverUrl, tagNames, totalTimeLimitMs); // Pass the full URL
                })
                .collect(Collectors.toList());

        // 5. Return the Page<QuizDTO>
        return new PageImpl<>(quizDTOs, pageable, quizPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuizDTO> getPublicPublishedQuizzes(Pageable pageable) {
        Page<Quiz> quizPage = quizRepository.findByVisibilityAndStatus(1, "PUBLISHED", pageable);
        List<Quiz> quizzes = quizPage.getContent();
        if (quizzes.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, quizPage.getTotalElements());
        }

        Set<UUID> creatorIds = quizzes.stream().map(Quiz::getCreatorId).collect(Collectors.toSet());
        Map<UUID, String> creatorUsernameMap = userAccountRepository.findAllById(creatorIds).stream()
                .collect(Collectors.toMap(UserAccount::getUserId, UserAccount::getUsername));

        Set<UUID> coverImageIds = quizzes.stream().map(Quiz::getCoverImageId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<UUID, String> coverImageUrlMap = imageStorageRepository.findAllById(coverImageIds).stream()
                .collect(Collectors.toMap(ImageStorage::getImageId, ImageStorage::getFilePath));

        List<UUID> quizIds = quizzes.stream().map(Quiz::getQuizId).collect(Collectors.toList());
        List<QuizTag> allQuizTags = quizTagRepository.findByQuizIdIn(quizIds);
        Set<UUID> allTagIds = allQuizTags.stream().map(QuizTag::getTagId).collect(Collectors.toSet());
        Map<UUID, String> tagNameMap = tagRepository.findAllById(allTagIds).stream()
                .collect(Collectors.toMap(Tag::getTagId, Tag::getName));
        Map<UUID, List<String>> tagsByQuizIdMap = allQuizTags.stream()
                .collect(Collectors.groupingBy(QuizTag::getQuizId,
                        Collectors.mapping(qt -> tagNameMap.get(qt.getTagId()), Collectors.toList())));

        Map<UUID, List<Question>> questionsByQuizIdMap = new HashMap<>();
        if (!quizIds.isEmpty()) {
            List<Question> allQuestionsForPageQuizzes = questionRepository.findByQuizIdIn(quizIds);
            questionsByQuizIdMap = allQuestionsForPageQuizzes.stream().collect(Collectors.groupingBy(Question::getQuizId));
        }
        Map<UUID, List<Question>> finalQuestionsMap = questionsByQuizIdMap;


        List<QuizDTO> quizDTOs = quizzes.stream()
                .map(quiz -> {
                    String creatorUsername = creatorUsernameMap.get(quiz.getCreatorId());
                    String coverFilePath = quiz.getCoverImageId() != null ? coverImageUrlMap.get(quiz.getCoverImageId()) : null;
                    String fullCoverUrl = null;
                    if (StringUtils.hasText(coverFilePath)) { // Check if filePath is not null or empty
                        fullCoverUrl = imageStorageService.getPublicUrl(coverFilePath); // Construct full URL
                    }
                    List<String> tagNames = tagsByQuizIdMap.getOrDefault(quiz.getQuizId(), Collections.emptyList());
                    List<Question> quizQuestions = finalQuestionsMap.getOrDefault(quiz.getQuizId(), Collections.emptyList());
                    int totalTimeLimitMs = quizQuestions.stream()
                            .filter(q -> q.getTimeLimit() != null)
                            .mapToInt(Question::getTimeLimit)
                            .sum();
                    return mapQuizEntityToListDTO(quiz, creatorUsername, fullCoverUrl, tagNames, totalTimeLimitMs); // Pass the full URL
                })
                .collect(Collectors.toList());

        return new PageImpl<>(quizDTOs, pageable, quizPage.getTotalElements());
    }

    // Mapper optimized for list view (no lobby video deserialization, no questions)
    private QuizDTO mapQuizEntityToListDTO(Quiz quiz, String creatorUsername, String coverImageUrl, List<String> tagNames, Integer totalTimeLimitMs) {
        if (quiz == null) return null;
        return QuizDTO.builder()
                .quizId(quiz.getQuizId())
                .creatorId(quiz.getCreatorId())
                .creatorUsername(creatorUsername)
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .visibility(quiz.getVisibility())
                .status(quiz.getStatus())
                .quizType(quiz.getQuizTypeInfo())
                .questionCount(quiz.getQuestionCount())
                .playCount(quiz.getPlayCount())
                .cover(coverImageUrl)
                .tags(tagNames)
                .created(quiz.getCreatedAt() != null ? quiz.getCreatedAt().toInstant().toEpochMilli() : null)
                .modified(quiz.getModifiedAt() != null ? quiz.getModifiedAt().toInstant().toEpochMilli() : null)
                .questions(Collections.emptyList())
                .isValid(true)
                .totalQuizTimeLimitMs(totalTimeLimitMs)
                .build();
    }

    private QuizDTO mapQuizEntityToDto(Quiz quiz, String creatorUsername,
                                       boolean loadQuestions, List<String> tagNames,
                                       boolean calculateTotalTimeLimit) {
        if (quiz == null) return null;

        QuizDTO.QuizDTOBuilder builder = QuizDTO.builder()
                .quizId(quiz.getQuizId())
                .creatorId(quiz.getCreatorId())
                .creatorUsername(creatorUsername)
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .visibility(quiz.getVisibility())
                .status(quiz.getStatus())
                .quizType(quiz.getQuizTypeInfo())
                .questionCount(quiz.getQuestionCount())
                .playCount(quiz.getPlayCount())
                .isValid(true)
                .tags(tagNames);

        // MODIFICATION: Populate cover URL from coverImageId
        if (quiz.getCoverImageId() != null) {
            builder.cover(imageStorageService.getPublicUrl(quiz.getCoverImageId()));
        }

        if (StringUtils.hasText(quiz.getLobbyVideoJson())) {
            try {
                builder.lobbyVideo(objectMapper.readValue(quiz.getLobbyVideoJson(), VideoDetailDTO.class));
            } catch (JsonProcessingException e) {
                log.error("Error deserializing lobby video for quiz {}: {}", quiz.getQuizId(), e.getMessage());
            }
        }
        if (quiz.getCreatedAt() != null) builder.created(quiz.getCreatedAt().toInstant().toEpochMilli());
        if (quiz.getModifiedAt() != null) builder.modified(quiz.getModifiedAt().toInstant().toEpochMilli());

        List<Question> quizQuestions = Collections.emptyList();
        if (loadQuestions || calculateTotalTimeLimit) {
            quizQuestions = questionRepository.findByQuizIdOrderByPositionAsc(quiz.getQuizId());
        }

        if (loadQuestions) {
            // Pass ImageStorageService to mapQuestionEntityToDto if it needs to resolve URLs internally
            // Or, resolve URLs here if mapQuestionEntityToDto only takes basic Question entity
            builder.questions(quizQuestions.stream()
                    .map(this::mapQuestionEntityToDto) // This DTO mapper also needs update
                    .collect(Collectors.toList()));
        } else {
            builder.questions(Collections.emptyList());
        }

        if (calculateTotalTimeLimit) {
            int totalTimeMs = quizQuestions.stream()
                    .filter(q -> q.getTimeLimit() != null)
                    .mapToInt(Question::getTimeLimit)
                    .sum();
            builder.totalQuizTimeLimitMs(totalTimeMs);
        }
        return builder.build();
    }

    // Helper method to map Question entity to QuestionDTO
    private QuestionDTO mapQuestionEntityToDto(Question question) {
        if (question == null) return null;

        QuestionDTO.QuestionDTOBuilder builder = QuestionDTO.builder()
                .id(question.getQuestionId())
                .type(question.getQuestionType())
                .title(question.getQuestionText())
                .description(question.getDescriptionText())
                .time(question.getTimeLimit())
                .pointsMultiplier(question.getPointsMultiplier())
                .position(question.getPosition())
                .media(Collections.emptyList()); // Default

        // MODIFICATION: Populate image URL from imageId
        if (question.getImageId() != null) {
            builder.image(imageStorageService.getPublicUrl(question.getImageId()));
        }

        if (question.getVideoContentJson() != null && !question.getVideoContentJson().isEmpty()) {
            try {
                VideoDetailDTO videoDto = objectMapper.readValue(question.getVideoContentJson(), VideoDetailDTO.class);
                builder.video(videoDto);
            } catch (JsonProcessingException e) {
                log.error("Error deserializing question video JSON: {}", e.getMessage());
            }
        }

        if (question.getAnswerDataJson() != null && !question.getAnswerDataJson().isEmpty()) {
            try {
                List<ChoiceDTO> choices = objectMapper.readValue(question.getAnswerDataJson(), new TypeReference<List<ChoiceDTO>>() {});
                builder.choices(choices);
            } catch (JsonProcessingException e) {
                log.error("Error deserializing question choices JSON: {}", e.getMessage());
                builder.choices(Collections.emptyList());
            }
        } else {
            builder.choices(Collections.emptyList());
        }
        return builder.build();
    }

    // --- Other existing QuizService method implementations ---
    @Override
    @Transactional(readOnly = true)
    public Optional<Quiz> getQuizById(UUID quizId) {
        return quizRepository.findById(quizId);
    }

    @Override
    @Transactional(readOnly = true)
    public Quiz getQuizById_Original(UUID quizId) {
        return quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz", "id", quizId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Quiz> getAllQuizzes(Pageable pageable) {
        return quizRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Quiz> getQuizzesByCreatorId(UUID creatorId, Pageable pageable) {
        return quizRepository.findByCreatorId(creatorId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Quiz> searchQuizzesByTitle(String title, Pageable pageable) {
        return quizRepository.findByTitleContainingIgnoreCase(title, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Quiz> getQuizzesByStatus(String status, Pageable pageable) {
        return quizRepository.findByStatus(status, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Quiz> getPublicQuizzes(Pageable pageable) {
        return quizRepository.findByVisibilityAndStatus(1, "PUBLISHED", pageable);
    }

    @Override
    @Transactional
    public Quiz updateQuiz(UUID quizId, Quiz quizDetails) {
        // This method still operates on entities.
        // For a full DTO-based update, a new service method would be needed.
        Quiz existingQuiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz", "id", quizId));

        existingQuiz.setTitle(quizDetails.getTitle());
        existingQuiz.setDescription(quizDetails.getDescription());
        // ... copy other relevant fields from quizDetails to existingQuiz ...
        // Manually set modifiedAt before saving
        existingQuiz.setModifiedAt(OffsetDateTime.now());
        return quizRepository.save(existingQuiz);
    }

    @Override
    @Transactional
    public Quiz updateQuizStatus(UUID quizId, String newStatus) {
        Quiz quiz = getQuizById_Original(quizId);
        quiz.setStatus(newStatus);
        // Manually set modifiedAt before saving
        quiz.setModifiedAt(OffsetDateTime.now());
        return quizRepository.save(quiz);
    }

    @Override
    @Transactional
    public Quiz updateQuizVisibility(UUID quizId, Integer newVisibility) {
        Quiz quiz = getQuizById_Original(quizId);
        quiz.setVisibility(newVisibility);
        // Manually set modifiedAt before saving
        quiz.setModifiedAt(OffsetDateTime.now());
        return quizRepository.save(quiz);
    }

    @Override
    @Transactional
    public void deleteQuiz(UUID quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz", "id", quizId));

        UUID creatorId = quiz.getCreatorId();
        long totalFreedSpace = 0;

        // 1. Delete Cover Image (if exists)
        if (quiz.getCoverImageId() != null) {
            try {
                totalFreedSpace += imageStorageService.deleteImageStorageAndFile(quiz.getCoverImageId());
            } catch (ResourceNotFoundException e) {
                log.warn("Cover ImageStorage record {} for quiz {} not found during deletion. File size not accounted for.", quiz.getCoverImageId(), quizId);
            } catch (FileStorageException e) {
                // Log and potentially re-throw or handle based on policy
                // If this fails, the transaction should roll back, quiz won't be deleted.
                log.error("Failed to delete cover image file for quiz {}: {}", quizId, e.getMessage());
                throw e; // Or a custom wrapper exception
            }
        }

        // 2. Delete Question Images
        List<Question> questions = questionRepository.findByQuizIdOrderByPositionAsc(quizId);
        for (Question question : questions) {
            if (question.getImageId() != null) {
                try {
                    totalFreedSpace += imageStorageService.deleteImageStorageAndFile(question.getImageId());
                } catch (ResourceNotFoundException e) {
                    log.warn("Question ImageStorage record {} for question {} (quiz {}) not found during deletion. File size not accounted for.",
                            question.getImageId(), question.getQuestionId(), quizId);
                } catch (FileStorageException e) {
                    log.error("Failed to delete image file for question {} (quiz {}): {}", question.getQuestionId(), quizId, e.getMessage());
                    throw e; // Or a custom wrapper exception
                }
            }
        }

        // 3. Delete associations and main entities
        // Order: QuizTags, Questions, then Quiz itself.
        // (Your existing deleteQuiz in QuizServiceImpl already has questionRepository.deleteByQuizId and quizTagRepository.deleteByQuizId)
        // Ensure these are called before quizRepository.deleteById(quizId)

        quizTagRepository.deleteByQuizId(quizId); // Ensure this runs
        questionRepository.deleteByQuizId(quizId); // Ensure this runs
        quizRepository.deleteById(quizId); // This will perform soft/hard delete based on Quiz entity

        // 4. Update User Storage
        if (totalFreedSpace > 0 && creatorId != null) {
            try {
                userAccountService.updateUserStorageUsed(creatorId, -totalFreedSpace); // Subtract freed space
            } catch (Exception e) {
                // CRITICAL: Quiz and files deleted, but storage update failed.
                // This needs careful logging and possibly manual reconciliation.
                log.error("CRITICAL: Quiz {} and its images deleted, but failed to update user {} storage by {}. Manual reconciliation needed. Error: {}",
                        quizId, creatorId, -totalFreedSpace, e.getMessage());
                // Don't re-throw here, as the primary deletion was successful.
            }
        }
        log.info("Quiz {} and its associated images deleted successfully. Freed space: {} bytes for user {}", quizId, totalFreedSpace, creatorId);
    }
}