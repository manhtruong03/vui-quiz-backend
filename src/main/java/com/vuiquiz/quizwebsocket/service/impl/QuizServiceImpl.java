package com.vuiquiz.quizwebsocket.service.impl;

import com.vuiquiz.quizwebsocket.exception.ResourceNotFoundException;
import com.vuiquiz.quizwebsocket.model.Quiz;
import com.vuiquiz.quizwebsocket.repository.QuizRepository;
import com.vuiquiz.quizwebsocket.service.QuizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class QuizServiceImpl implements com.vuiquiz.quizwebsocket.service.QuizService {

    private final QuizRepository quizRepository;
    // private final UserAccountService userAccountService; // Autowire if needed
    // private final ImageStorageService imageStorageService; // Autowire if needed

    @Autowired
    public QuizServiceImpl(QuizRepository quizRepository) {
        this.quizRepository = quizRepository;
    }

    @Override
    @Transactional
    public com.vuiquiz.quizwebsocket.model.Quiz createQuiz(com.vuiquiz.quizwebsocket.model.Quiz quiz) {
        // Validate creatorId exists
        // userAccountService.getUserById(quiz.getCreatorId())
        //     .orElseThrow(() -> new ResourceNotFoundException("UserAccount", "id", quiz.getCreatorId()));
        // Validate coverImageId if present
        // if (quiz.getCoverImageId() != null) {
        //     imageStorageService.getImageStorageById(quiz.getCoverImageId())
        //         .orElseThrow(() -> new ResourceNotFoundException("ImageStorage", "id", quiz.getCoverImageId()));
        // }
        quiz.setQuizId(null);
        return quizRepository.save(quiz);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<com.vuiquiz.quizwebsocket.model.Quiz> getQuizById(UUID quizId) {
        return quizRepository.findById(quizId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<com.vuiquiz.quizwebsocket.model.Quiz> getAllQuizzes(Pageable pageable) {
        return quizRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<com.vuiquiz.quizwebsocket.model.Quiz> getQuizzesByCreatorId(UUID creatorId, Pageable pageable) {
        return quizRepository.findByCreatorId(creatorId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<com.vuiquiz.quizwebsocket.model.Quiz> searchQuizzesByTitle(String title, Pageable pageable) {
        return quizRepository.findByTitleContainingIgnoreCase(title, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<com.vuiquiz.quizwebsocket.model.Quiz> getQuizzesByStatus(String status, Pageable pageable) {
        return quizRepository.findByStatus(status, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<com.vuiquiz.quizwebsocket.model.Quiz> getPublicQuizzes(Pageable pageable) {
        // Assuming 1 means PUBLIC and status is "PUBLISHED"
        return quizRepository.findByVisibilityAndStatus(1, "PUBLISHED", pageable);
        // Or use the custom query: return quizRepository.findPublicAndPublished(pageable);
    }


    @Override
    @Transactional
    public com.vuiquiz.quizwebsocket.model.Quiz updateQuiz(UUID quizId, com.vuiquiz.quizwebsocket.model.Quiz quizDetails) {
        com.vuiquiz.quizwebsocket.model.Quiz existingQuiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz", "id", quizId));

        // Validate new creatorId if changed and allowed
        // Validate new coverImageId if changed
        existingQuiz.setTitle(quizDetails.getTitle());
        existingQuiz.setDescription(quizDetails.getDescription());
        existingQuiz.setLobbyVideoJson(quizDetails.getLobbyVideoJson());
        existingQuiz.setCountdownTimer(quizDetails.getCountdownTimer());
        // questionCount is typically derived, not set directly
        // playCount, favoriteCount are usually incremented by other actions
        existingQuiz.setStatus(quizDetails.getStatus());
        existingQuiz.setVisibility(quizDetails.getVisibility());
        if (quizDetails.getCoverImageId() != null) { // Allow setting to null or new valid ID
            // imageStorageService.getImageStorageById(quizDetails.getCoverImageId())
            //  .orElseThrow(() -> new ResourceNotFoundException("ImageStorage", "id", quizDetails.getCoverImageId()));
            existingQuiz.setCoverImageId(quizDetails.getCoverImageId());
        } else {
            existingQuiz.setCoverImageId(null);
        }
        // creatorId should generally not change or be handled with care
        return quizRepository.save(existingQuiz);
    }

    @Override
    @Transactional
    public void deleteQuiz(UUID quizId) {
        com.vuiquiz.quizwebsocket.model.Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz", "id", quizId));
        // Related questions, quizTags will be deleted by DB CASCADE if ON DELETE CASCADE is set
        // Or handle deletion in their respective services if not using DB cascade for logic
        quizRepository.delete(quiz);
    }

    @Override
    @Transactional
    public com.vuiquiz.quizwebsocket.model.Quiz updateQuizStatus(UUID quizId, String newStatus) {
        com.vuiquiz.quizwebsocket.model.Quiz quiz = getQuizById(quizId).orElseThrow(() -> new ResourceNotFoundException("Quiz", "id", quizId));
        quiz.setStatus(newStatus);
        // Add validation for allowed status transitions if necessary
        return quizRepository.save(quiz);
    }

    @Override
    @Transactional
    public com.vuiquiz.quizwebsocket.model.Quiz updateQuizVisibility(UUID quizId, Integer newVisibility) {
        com.vuiquiz.quizwebsocket.model.Quiz quiz = getQuizById(quizId).orElseThrow(() -> new ResourceNotFoundException("Quiz", "id", quizId));
        quiz.setVisibility(newVisibility);
        // Add validation for visibility values if necessary
        return quizRepository.save(quiz);
    }
}

