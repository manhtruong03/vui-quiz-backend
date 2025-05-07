package com.vuiquiz.quizwebsocket.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vuiquiz.quizwebsocket.dto.ChoiceDTO;
import com.vuiquiz.quizwebsocket.dto.QuestionDTO;
import com.vuiquiz.quizwebsocket.dto.QuizDTO;
import com.vuiquiz.quizwebsocket.dto.VideoDetailDTO;
import com.vuiquiz.quizwebsocket.exception.ResourceNotFoundException;
import com.vuiquiz.quizwebsocket.model.*;
import com.vuiquiz.quizwebsocket.repository.*;
import com.vuiquiz.quizwebsocket.security.services.UserDetailsImpl;
import com.vuiquiz.quizwebsocket.service.ImageStorageService;
import com.vuiquiz.quizwebsocket.service.QuizService;
import com.vuiquiz.quizwebsocket.service.QuizTagService;
import com.vuiquiz.quizwebsocket.service.TagService;
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

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class QuizServiceImpl implements QuizService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final ImageStorageService imageStorageService;
    private final ObjectMapper objectMapper;
    private final UserAccountRepository userAccountRepository;
    private final ImageStorageRepository imageStorageRepository;
    private final TagService tagService; // Inject TagService
    private final QuizTagService quizTagService; // Inject QuizTagService
    private final QuizTagRepository quizTagRepository; // Inject Repositories for batch fetching
    private final TagRepository tagRepository;         // Inject Repositories for batch fetching


    @Autowired
    public QuizServiceImpl(QuizRepository quizRepository,
                           QuestionRepository questionRepository,
                           ImageStorageService imageStorageService,
                           ObjectMapper objectMapper,
                           UserAccountRepository userAccountRepository,
                           ImageStorageRepository imageStorageRepository,
                           TagService tagService,               // Add to constructor
                           QuizTagService quizTagService,         // Add to constructor
                           QuizTagRepository quizTagRepository,   // Add to constructor
                           TagRepository tagRepository) {           // Add to constructor
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.imageStorageService = imageStorageService;
        this.objectMapper = objectMapper;
        this.userAccountRepository = userAccountRepository;
        this.imageStorageRepository = imageStorageRepository;
        this.tagService = tagService;                 // Assign
        this.quizTagService = quizTagService;         // Assign
        this.quizTagRepository = quizTagRepository;   // Assign
        this.tagRepository = tagRepository;           // Assign
    }

    @Override
    @Transactional
    public QuizDTO createQuiz(QuizDTO quizDto, UUID creatorId) {
        // ... (createQuiz implementation from Phase 3 remains the same)
        // The response from createQuiz in Phase 3 intentionally returns empty questions.
        // This behavior is fine for create, as the primary goal is creation.
        // The full details are retrieved via getQuizDetailsById.
        Quiz quiz = new Quiz();
        quiz.setCreatorId(creatorId);
        quiz.setTitle(quizDto.getTitle());
        quiz.setDescription(quizDto.getDescription());
        quiz.setVisibility(quizDto.getVisibility() != null ? quizDto.getVisibility() : 0);
        quiz.setStatus(quizDto.getStatus() != null && !quizDto.getStatus().trim().isEmpty() ? quizDto.getStatus() : "DRAFT");
        quiz.setQuizTypeInfo(quizDto.getQuizType());

        if (quizDto.getCover() != null && !quizDto.getCover().trim().isEmpty()) {
            ImageStorage coverImage = imageStorageService.findOrCreateByFilePath(quizDto.getCover(), creatorId);
            if (coverImage != null) {
                quiz.setCoverImageId(coverImage.getImageId());
            }
        }

        if (quizDto.getLobbyVideo() != null) {
            try {
                quiz.setLobbyVideoJson(objectMapper.writeValueAsString(quizDto.getLobbyVideo()));
            } catch (JsonProcessingException e) {
                log.error("Error serializing lobby video DTO for quiz title '{}': {}", quizDto.getTitle(), e.getMessage());
                quiz.setLobbyVideoJson(null);
            }
        }

        Quiz savedQuiz = quizRepository.save(quiz);
        UUID persistedQuizId = savedQuiz.getQuizId();
        int questionCount = 0;

        if (!CollectionUtils.isEmpty(quizDto.getQuestions())) {
            List<Question> questionsToSave = new ArrayList<>();
            for (QuestionDTO questionDtoInternal : quizDto.getQuestions()) { // Renamed to avoid conflict
                Question question = new Question();
                question.setQuizId(persistedQuizId);
                question.setQuestionType(questionDtoInternal.getType());
                question.setQuestionText(questionDtoInternal.getTitle());
                question.setDescriptionText(questionDtoInternal.getDescription());
                question.setTimeLimit(questionDtoInternal.getTime());
                question.setPointsMultiplier(questionDtoInternal.getPointsMultiplier());
                question.setPosition(questionDtoInternal.getPosition() != null ? questionDtoInternal.getPosition() : questionCount);

                if (questionDtoInternal.getImage() != null && !questionDtoInternal.getImage().trim().isEmpty()) {
                    ImageStorage questionImage = imageStorageService.findOrCreateByFilePath(questionDtoInternal.getImage(), creatorId);
                    if (questionImage != null) {
                        question.setImageId(questionImage.getImageId());
                    }
                }

                if (questionDtoInternal.getVideo() != null) {
                    try {
                        question.setVideoContentJson(objectMapper.writeValueAsString(questionDtoInternal.getVideo()));
                    } catch (JsonProcessingException e) {
                        log.error("Error serializing question video DTO to JSON for question title '{}': {}", questionDtoInternal.getTitle(), e.getMessage());
                        question.setVideoContentJson(null);
                    }
                }

                if (!CollectionUtils.isEmpty(questionDtoInternal.getChoices())) {
                    try {
                        question.setAnswerDataJson(objectMapper.writeValueAsString(questionDtoInternal.getChoices()));
                    } catch (JsonProcessingException e) {
                        log.error("Error serializing question choices DTO to JSON for question title '{}': {}", questionDtoInternal.getTitle(), e.getMessage());
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

        // --- NEW: Handle Tags ---
        if (!CollectionUtils.isEmpty(quizDto.getTags())) {
            for (String tagName : quizDto.getTags()) {
                try {
                    Tag tag = tagService.findOrCreateTagByName(tagName);
                    quizTagService.addTagToQuiz(persistedQuizId, tag.getTagId());
                } catch (IllegalArgumentException e) {
                    log.warn("Skipping invalid tag name '{}' for quiz '{}': {}", tagName, quizDto.getTitle(), e.getMessage());
                } catch (ResourceNotFoundException e) {
                    log.error("Error associating tag '{}' for quiz '{}', tag/quiz likely not found: {}", tagName, quizDto.getTitle(), e.getMessage());
                    // Or rethrow / handle differently
                }
            }
        }

        savedQuiz.setQuestionCount(questionCount);
        Quiz finalSavedQuiz = quizRepository.save(savedQuiz);

        String creatorUsername = userAccountRepository.findById(finalSavedQuiz.getCreatorId())
                .map(UserAccount::getUsername)
                .orElse(null);

        QuizDTO responseDto = mapQuizEntityToDto(finalSavedQuiz, creatorUsername, false, Collections.emptyList()); // Pass empty tags list for create response
        return responseDto;
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

        return mapQuizEntityToDto(quiz, creatorUsername, true, tagNames); // Pass tag names, load questions = true
    }

    // Helper to get tag names
    private List<String> getTagNamesForQuiz(UUID quizId) {
        List<QuizTag> quizTags = quizTagRepository.findByQuizId(quizId);
        if (quizTags.isEmpty()) {
            return Collections.emptyList();
        }
        Set<UUID> tagIds = quizTags.stream().map(QuizTag::getTagId).collect(Collectors.toSet());
        List<Tag> tags = tagRepository.findAllById(tagIds);
        return tags.stream().map(Tag::getName).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuizDTO> getQuizzesByCurrentUser(Pageable pageable) {
        // 1. Get current user's ID
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
            // Handle cases where user is not authenticated appropriately
            // Option 1: Throw exception
            // throw new IllegalStateException("User must be authenticated to fetch their quizzes.");
            // Option 2: Return empty page
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        UUID currentUserId = userDetails.getId();
        String currentUsername = userDetails.getUsername(); // Get username directly

        // 2. Fetch the page of Quiz entities for this user
        Page<Quiz> quizPage = quizRepository.findByCreatorId(currentUserId, pageable);

        List<Quiz> quizzes = quizPage.getContent();
        if (quizzes.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, quizPage.getTotalElements());
        }

        // 3. Efficiently fetch related data (only need covers and tags, username is known)
        // Fetch Cover Image FilePaths
        Set<UUID> coverImageIds = quizzes.stream()
                .map(Quiz::getCoverImageId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<UUID, String> coverImageUrlMap = imageStorageRepository.findAllById(coverImageIds).stream()
                .collect(Collectors.toMap(ImageStorage::getImageId, ImageStorage::getFilePath));

        // Fetch Tags
        List<UUID> quizIds = quizzes.stream().map(Quiz::getQuizId).collect(Collectors.toList());
        List<QuizTag> allQuizTags = quizTagRepository.findByQuizIdIn(quizIds);
        Set<UUID> allTagIds = allQuizTags.stream().map(QuizTag::getTagId).collect(Collectors.toSet());
        Map<UUID, String> tagNameMap = tagRepository.findAllById(allTagIds).stream()
                .collect(Collectors.toMap(Tag::getTagId, Tag::getName));
        Map<UUID, List<String>> tagsByQuizIdMap = allQuizTags.stream()
                .collect(Collectors.groupingBy(
                        QuizTag::getQuizId,
                        Collectors.mapping(qt -> tagNameMap.get(qt.getTagId()), Collectors.toList())
                ));

        // 4. Map Quiz entities to QuizDTOs
        List<QuizDTO> quizDTOs = quizzes.stream()
                .map(quiz -> {
                    // Username is known for the current user
                    String coverImageUrl = quiz.getCoverImageId() != null ? coverImageUrlMap.get(quiz.getCoverImageId()) : null;
                    List<String> tagNames = tagsByQuizIdMap.getOrDefault(quiz.getQuizId(), Collections.emptyList());
                    // Use the list mapper helper
                    return mapQuizEntityToListDTO(quiz, currentUsername, coverImageUrl, tagNames);
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

        // Efficiently fetch related data
        Set<UUID> creatorIds = quizzes.stream().map(Quiz::getCreatorId).collect(Collectors.toSet());
        Map<UUID, String> creatorUsernameMap = userAccountRepository.findAllById(creatorIds).stream()
                .collect(Collectors.toMap(UserAccount::getUserId, UserAccount::getUsername));

        Set<UUID> coverImageIds = quizzes.stream().map(Quiz::getCoverImageId).filter(id -> id != null).collect(Collectors.toSet());
        Map<UUID, String> coverImageUrlMap = imageStorageRepository.findAllById(coverImageIds).stream()
                .collect(Collectors.toMap(ImageStorage::getImageId, ImageStorage::getFilePath));

        // Fetch Tags efficiently for all quizzes in the page
        List<UUID> quizIds = quizzes.stream().map(Quiz::getQuizId).collect(Collectors.toList());
        List<QuizTag> allQuizTags = quizTagRepository.findByQuizIdIn(quizIds); // Need this method in QuizTagRepository
        Set<UUID> allTagIds = allQuizTags.stream().map(QuizTag::getTagId).collect(Collectors.toSet());
        Map<UUID, String> tagNameMap = tagRepository.findAllById(allTagIds).stream()
                .collect(Collectors.toMap(Tag::getTagId, Tag::getName));

        // Group tag names by quizId
        Map<UUID, List<String>> tagsByQuizIdMap = allQuizTags.stream()
                .collect(Collectors.groupingBy(
                        QuizTag::getQuizId,
                        Collectors.mapping(qt -> tagNameMap.get(qt.getTagId()), Collectors.toList())
                ));


        List<QuizDTO> quizDTOs = quizzes.stream()
                .map(quiz -> {
                    String creatorUsername = creatorUsernameMap.get(quiz.getCreatorId());
                    String coverImageUrl = quiz.getCoverImageId() != null ? coverImageUrlMap.get(quiz.getCoverImageId()) : null;
                    List<String> tagNames = tagsByQuizIdMap.getOrDefault(quiz.getQuizId(), Collections.emptyList());
                    // Use a specialized mapper or adapt the existing one for list view
                    return mapQuizEntityToListDTO(quiz, creatorUsername, coverImageUrl, tagNames); // Pass tags
                })
                .collect(Collectors.toList());

        return new PageImpl<>(quizDTOs, pageable, quizPage.getTotalElements());
    }

    // Mapper optimized for list view (no lobby video deserialization, no questions)
    private QuizDTO mapQuizEntityToListDTO(Quiz quiz, String creatorUsername, String coverImageUrl, List<String> tagNames) {
        if (quiz == null) {
            return null;
        }
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
                .tags(tagNames) // Set tags here
                .created(quiz.getCreatedAt() != null ? quiz.getCreatedAt().toInstant().toEpochMilli() : null)
                .modified(quiz.getModifiedAt() != null ? quiz.getModifiedAt().toInstant().toEpochMilli() : null)
                .questions(Collections.emptyList())
                .isValid(true)
                .build();
    }

    // Main mapper method, now with a flag to load questions
    private QuizDTO mapQuizEntityToDto(Quiz quiz, String creatorUsername, boolean loadQuestions, List<String> tagNames) {
        if (quiz == null) {
            return null;
        }

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
                .playAsGuest(null)
                .type(quiz.getQuizTypeInfo())
                .isValid(true)
                .tags(tagNames); // Set the tags

        // ... (mapping for cover, lobby video, created, modified remains the same) ...
        if (quiz.getCoverImageId() != null) {
            imageStorageService.getImageStorageById(quiz.getCoverImageId())
                    .ifPresent(img -> builder.cover(img.getFilePath()));
        }
        if (quiz.getLobbyVideoJson() != null && !quiz.getLobbyVideoJson().isEmpty()) {
            try {
                builder.lobbyVideo(objectMapper.readValue(quiz.getLobbyVideoJson(), VideoDetailDTO.class));
            } catch (JsonProcessingException e) {
                log.error("Error deserializing lobby video: {}", e.getMessage());
            }
        }
        if (quiz.getCreatedAt() != null) builder.created(quiz.getCreatedAt().toInstant().toEpochMilli());
        if (quiz.getModifiedAt() != null) builder.modified(quiz.getModifiedAt().toInstant().toEpochMilli());

        if (loadQuestions) {
            List<Question> questions = questionRepository.findByQuizIdOrderByPositionAsc(quiz.getQuizId());
            if (!CollectionUtils.isEmpty(questions)) {
                builder.questions(questions.stream()
                        .map(this::mapQuestionEntityToDto) // Use helper
                        .collect(Collectors.toList()));
            } else {
                builder.questions(Collections.emptyList());
            }
        } else {
            builder.questions(Collections.emptyList()); // Explicitly set empty if not loading
        }

        return builder.build();
    }

    // Helper method to map Question entity to QuestionDTO
    private QuestionDTO mapQuestionEntityToDto(Question question) {
        if (question == null) {
            return null;
        }

        QuestionDTO.QuestionDTOBuilder builder = QuestionDTO.builder()
                .id(question.getQuestionId())
                .type(question.getQuestionType())
                .title(question.getQuestionText()) // Map Question.questionText to DTO.title
                .description(question.getDescriptionText()) // Map Question.descriptionText to DTO.description
                .time(question.getTimeLimit())
                .pointsMultiplier(question.getPointsMultiplier())
                .position(question.getPosition())
                .media(Collections.emptyList()); // Assuming media is not yet handled, default to empty

        if (question.getImageId() != null) {
            imageStorageService.getImageStorageById(question.getImageId())
                    .ifPresent(img -> builder.image(img.getFilePath()));
        }

        if (question.getVideoContentJson() != null && !question.getVideoContentJson().isEmpty()) {
            try {
                VideoDetailDTO videoDto = objectMapper.readValue(question.getVideoContentJson(), VideoDetailDTO.class);
                builder.video(videoDto);
            } catch (JsonProcessingException e) {
                log.error("Error deserializing question video JSON to DTO for questionId '{}': {}", question.getQuestionId(), e.getMessage());
            }
        }

        if (question.getAnswerDataJson() != null && !question.getAnswerDataJson().isEmpty()) {
            try {
                List<ChoiceDTO> choices = objectMapper.readValue(question.getAnswerDataJson(), new TypeReference<List<ChoiceDTO>>() {
                });
                builder.choices(choices);
            } catch (JsonProcessingException e) {
                log.error("Error deserializing question choices JSON to DTO for questionId '{}': {}", question.getQuestionId(), e.getMessage());
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
        if (quizRepository.existsById(quizId)) {
            questionRepository.deleteByQuizId(quizId);
            quizTagRepository.deleteByQuizId(quizId);
            quizRepository.deleteById(quizId);
        } else {
            throw new ResourceNotFoundException("Quiz", "id", quizId);
        }
    } // Added deleteByQuizId for QuizTag

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