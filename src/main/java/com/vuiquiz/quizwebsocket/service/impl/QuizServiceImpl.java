package com.vuiquiz.quizwebsocket.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference; // For deserializing List<ChoiceDTO>
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vuiquiz.quizwebsocket.dto.ChoiceDTO;
import com.vuiquiz.quizwebsocket.dto.QuestionDTO;
import com.vuiquiz.quizwebsocket.dto.QuizDTO;
import com.vuiquiz.quizwebsocket.dto.VideoDetailDTO;
import com.vuiquiz.quizwebsocket.exception.ResourceNotFoundException;
import com.vuiquiz.quizwebsocket.model.ImageStorage;
import com.vuiquiz.quizwebsocket.model.Question;
import com.vuiquiz.quizwebsocket.model.Quiz;
import com.vuiquiz.quizwebsocket.model.UserAccount;
import com.vuiquiz.quizwebsocket.repository.QuestionRepository;
import com.vuiquiz.quizwebsocket.repository.QuizRepository;
import com.vuiquiz.quizwebsocket.repository.UserAccountRepository;
import com.vuiquiz.quizwebsocket.service.ImageStorageService;
import com.vuiquiz.quizwebsocket.service.QuizService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class QuizServiceImpl implements QuizService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final ImageStorageService imageStorageService;
    private final ObjectMapper objectMapper;
    private final UserAccountRepository userAccountRepository;

    @Autowired
    public QuizServiceImpl(QuizRepository quizRepository,
                           QuestionRepository questionRepository,
                           ImageStorageService imageStorageService,
                           ObjectMapper objectMapper,
                           UserAccountRepository userAccountRepository) {
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.imageStorageService = imageStorageService;
        this.objectMapper = objectMapper;
        this.userAccountRepository = userAccountRepository;
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
                question.setPosition(questionDtoInternal.getPosition() !=null ? questionDtoInternal.getPosition() : questionCount);

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

        savedQuiz.setQuestionCount(questionCount);
        Quiz finalSavedQuiz = quizRepository.save(savedQuiz);

        String creatorUsername = userAccountRepository.findById(finalSavedQuiz.getCreatorId())
                .map(UserAccount::getUsername)
                .orElse(null);

        QuizDTO responseDto = mapQuizEntityToDto(finalSavedQuiz, creatorUsername, false); // Don't load questions for create response
        return responseDto;
    }

    @Override
    @Transactional(readOnly = true)
    public QuizDTO getQuizDetailsById(UUID quizId) { // Renamed method
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz", "id", quizId));

        String creatorUsername = userAccountRepository.findById(quiz.getCreatorId())
                .map(UserAccount::getUsername)
                .orElse(null);

        return mapQuizEntityToDto(quiz, creatorUsername, true); // Load questions = true
    }

    // Main mapper method, now with a flag to load questions
    private QuizDTO mapQuizEntityToDto(Quiz quiz, String creatorUsername, boolean loadQuestions) {
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
                .playAsGuest(null) // Mock data field, not in entity
                .type(quiz.getQuizTypeInfo()) // Mock data field, mapped from quizTypeInfo
                .isValid(true); // Assuming valid if successfully retrieved

        if (quiz.getCoverImageId() != null) {
            imageStorageService.getImageStorageById(quiz.getCoverImageId())
                    .ifPresent(img -> builder.cover(img.getFilePath()));
        }

        if (quiz.getLobbyVideoJson() != null && !quiz.getLobbyVideoJson().isEmpty()) {
            try {
                VideoDetailDTO lobbyVideoDto = objectMapper.readValue(quiz.getLobbyVideoJson(), VideoDetailDTO.class);
                builder.lobbyVideo(lobbyVideoDto);
            } catch (JsonProcessingException e) {
                log.error("Error deserializing lobby video JSON to DTO for quizId '{}': {}", quiz.getQuizId(), e.getMessage());
            }
        }

        if (quiz.getCreatedAt() != null) {
            builder.created(quiz.getCreatedAt().toInstant().toEpochMilli());
        }
        if (quiz.getModifiedAt() != null) {
            builder.modified(quiz.getModifiedAt().toInstant().toEpochMilli());
        }

        if (loadQuestions) {
            List<Question> questions = questionRepository.findByQuizIdOrderByPositionAsc(quiz.getQuizId());
            if (!CollectionUtils.isEmpty(questions)) {
                builder.questions(questions.stream()
                        .map(this::mapQuestionEntityToDto) // Use new helper
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
                List<ChoiceDTO> choices = objectMapper.readValue(question.getAnswerDataJson(), new TypeReference<List<ChoiceDTO>>() {});
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
            // Important: Delete questions first due to manual FK management
            questionRepository.deleteByQuizId(quizId); // Ensure this method exists and works
            quizRepository.deleteById(quizId);
        } else {
            throw new ResourceNotFoundException("Quiz", "id", quizId);
        }
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