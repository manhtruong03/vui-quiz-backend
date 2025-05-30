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
    public QuizDTO createQuiz(QuizDTO quizDto, UUID creatorId,
                              MultipartFile coverImageFile, List<MultipartFile> questionImageFiles) {

        long totalUploadSize = 0;
        if (coverImageFile != null && !coverImageFile.isEmpty()) {
            totalUploadSize += coverImageFile.getSize();
        }
        if (questionImageFiles != null) {
            for (MultipartFile qFile : questionImageFiles) {
                if (qFile != null && !qFile.isEmpty()) {
                    totalUploadSize += qFile.getSize();
                }
            }
        }

        // 0. Check total storage quota FIRST
        if (totalUploadSize > 0) {
            if (!userAccountService.canUserUpload(creatorId, totalUploadSize)) {
                throw new StorageQuotaExceededException("Upload exceeds storage quota. Required: " + totalUploadSize + " bytes.");
            }
        }

        Quiz quiz = new Quiz();
        // ... (map quizDto to quiz entity: creatorId, title, description, etc.)
        quiz.setCreatorId(creatorId);
        quiz.setTitle(quizDto.getTitle());
        quiz.setDescription(quizDto.getDescription());
        quiz.setVisibility(quizDto.getVisibility() != null ? quizDto.getVisibility() : 0);
        quiz.setStatus(StringUtils.hasText(quizDto.getStatus()) ? quizDto.getStatus() : "DRAFT");
        quiz.setQuizTypeInfo(quizDto.getQuizType());


        // 1. Handle Cover Image (if present)
        UUID savedCoverImageId = null;
        if (coverImageFile != null && !coverImageFile.isEmpty()) {
            String storedCoverFileName = fileStorageService.storeFile(coverImageFile);
            ImageStorage coverImageRecord = imageStorageService.createImageRecord(coverImageFile, storedCoverFileName, creatorId);
            quiz.setCoverImageId(coverImageRecord.getImageId());
            savedCoverImageId = coverImageRecord.getImageId(); // Keep track if successful
        }

        if (quizDto.getLobbyVideo() != null) {
            try {
                quiz.setLobbyVideoJson(objectMapper.writeValueAsString(quizDto.getLobbyVideo()));
            } catch (JsonProcessingException e) {
                log.error("Error serializing lobby video DTO for quiz title '{}': {}", quizDto.getTitle(), e.getMessage());
                quiz.setLobbyVideoJson(null);
            }
        }

        // ... (lobbyVideoJson handling)

        Quiz savedQuiz = quizRepository.save(quiz);
        UUID persistedQuizId = savedQuiz.getQuizId();
        int questionCount = 0;
        List<UUID> savedQuestionImageIds = new ArrayList<>(); // Keep track

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
                // ... (map other question DTO fields to question entity)
                question.setQuestionType(questionDtoInternal.getType());
                question.setQuestionText(questionDtoInternal.getTitle());
                question.setDescriptionText(questionDtoInternal.getDescription());
                question.setTimeLimit(questionDtoInternal.getTime() != null ? questionDtoInternal.getTime() : 0);
                question.setPointsMultiplier(questionDtoInternal.getPointsMultiplier() != null ? questionDtoInternal.getPointsMultiplier() : 0);
                question.setPosition(questionDtoInternal.getPosition() != null ? questionDtoInternal.getPosition() : i);


                // 2. Handle Question Image
                if (questionImageFiles != null && i < questionImageFiles.size()) {
                    MultipartFile questionImageFile = questionImageFiles.get(i);
                    if (questionImageFile != null && !questionImageFile.isEmpty()) {
                        String storedQuestionImageName = fileStorageService.storeFile(questionImageFile);
                        ImageStorage questionImageRecord = imageStorageService.createImageRecord(questionImageFile, storedQuestionImageName, creatorId);
                        question.setImageId(questionImageRecord.getImageId());
                        savedQuestionImageIds.add(questionImageRecord.getImageId()); // Keep track
                    }
                }
                // ... (map video, choices JSON for question)

                if (questionDtoInternal.getVideo() != null) {
                    try {
                        question.setVideoContentJson(objectMapper.writeValueAsString(questionDtoInternal.getVideo()));
                    } catch (JsonProcessingException e) {
                        log.error("Error serializing question video DTO: {}", e.getMessage());
                    }
                }
                if (!CollectionUtils.isEmpty(questionDtoInternal.getChoices())) {
                    try {
                        question.setAnswerDataJson(objectMapper.writeValueAsString(questionDtoInternal.getChoices()));
                    } catch (JsonProcessingException e) {
                        log.error("Error serializing question choices DTO: {}", e.getMessage());
                        question.setAnswerDataJson("[]");
                    }
                } else {
                    question.setAnswerDataJson("[]");
                }

                questionsToSave.add(question);
                questionCount++;
            }
            if (!questionsToSave.isEmpty()) {
                questionRepository.saveAll(questionsToSave);
            }
        }

        // Handle Tags (existing logic)
        if (!CollectionUtils.isEmpty(quizDto.getTags())) {
            for (String tagName : quizDto.getTags()) {
                try {
                    Tag tag = tagService.findOrCreateTagByName(tagName);
                    quizTagService.addTagToQuiz(persistedQuizId, tag.getTagId());
                } catch (Exception e) {
                    log.warn("Skipping tag '{}' for quiz '{}' due to error: {}", tagName, quizDto.getTitle(), e.getMessage());
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
                // This is tricky. Quiz and images are saved. Storage update failed.
                // Option 1: Log critical error, manual reconciliation needed. (Simplest for now)
                // Option 2: Try to roll back image storage (delete files, delete ImageStorage records) - complex.
                // Option 3: For a truly atomic operation, this might require distributed transaction concepts or a saga pattern,
                //           which is overkill for this stage.
                log.error("CRITICAL: Quiz {} created and images stored, but failed to update user {} storage by {}. Manual reconciliation needed. Error: {}",
                        finalSavedQuiz.getQuizId(), creatorId, totalUploadSize, e.getMessage());
                // For now, we let the quiz creation succeed and log the storage update issue.
            }
        }

        return mapQuizEntityToDto(finalSavedQuiz,
                userAccountRepository.findById(creatorId).map(UserAccount::getUsername).orElse(null),
                false, getTagNamesForQuiz(finalSavedQuiz.getQuizId()), false);
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
                    String coverImageUrl = quiz.getCoverImageId() != null ? coverImageUrlMap.get(quiz.getCoverImageId()) : null;
                    List<String> tagNames = tagsByQuizIdMap.getOrDefault(quiz.getQuizId(), Collections.emptyList());

                    // --- TWEAK 2: Calculate total time limit ---
                    List<Question> quizQuestions = finalQuestionsMap.getOrDefault(quiz.getQuizId(), Collections.emptyList());
                    int totalTimeLimitMs = quizQuestions.stream()
                            .filter(q -> q.getTimeLimit() != null)
                            .mapToInt(Question::getTimeLimit)
                            .sum();
                    // --- END TWEAK 2 CALCULATION ---

                    return mapQuizEntityToListDTO(quiz, currentUsername, coverImageUrl, tagNames, totalTimeLimitMs);
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
                    String coverImageUrl = quiz.getCoverImageId() != null ? coverImageUrlMap.get(quiz.getCoverImageId()) : null;
                    List<String> tagNames = tagsByQuizIdMap.getOrDefault(quiz.getQuizId(), Collections.emptyList());
                    List<Question> quizQuestions = finalQuestionsMap.getOrDefault(quiz.getQuizId(), Collections.emptyList());
                    int totalTimeLimitMs = quizQuestions.stream()
                            .filter(q -> q.getTimeLimit() != null)
                            .mapToInt(Question::getTimeLimit)
                            .sum();
                    return mapQuizEntityToListDTO(quiz, creatorUsername, coverImageUrl, tagNames, totalTimeLimitMs);
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
                .cover(coverImageUrl)
                .tags(tagNames)
                .created(quiz.getCreatedAt() != null ? quiz.getCreatedAt().toInstant().toEpochMilli() : null)
                .modified(quiz.getModifiedAt() != null ? quiz.getModifiedAt().toInstant().toEpochMilli() : null)
                .questions(Collections.emptyList()) // For list DTO, questions are not detailed
                .isValid(true) // Assuming valid if fetched
                .totalQuizTimeLimitMs(totalTimeLimitMs) // Set the new field
                .build();
    }

    // Main mapper method, now with a flag to load questions
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
        return quizRepository.save(existingQuiz);
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

    @Override
    @Transactional
    public Quiz updateQuizStatus(UUID quizId, String newStatus) {
        Quiz quiz = getQuizById_Original(quizId);
        quiz.setStatus(newStatus);
        return quizRepository.save(quiz);
    }

    @Override
    @Transactional
    public Quiz updateQuizVisibility(UUID quizId, Integer newVisibility) {
        Quiz quiz = getQuizById_Original(quizId);
        quiz.setVisibility(newVisibility);
        return quizRepository.save(quiz);
    }
}