package com.vuiquiz.quizwebsocket.service;

import com.vuiquiz.quizwebsocket.dto.QuizDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.UUID;

public interface QuizService {
    QuizDTO createQuiz(QuizDTO quizDto, UUID creatorId);

    // Renamed for Phase 4 to reflect returning full details
    QuizDTO getQuizDetailsById(UUID quizId);

    // Internal entity fetching method (if still needed, or can be removed if not used elsewhere)
    Optional<com.vuiquiz.quizwebsocket.model.Quiz> getQuizById(UUID quizId);

    // Keep other method signatures as they are
    com.vuiquiz.quizwebsocket.model.Quiz getQuizById_Original(UUID quizId);
    Page<com.vuiquiz.quizwebsocket.model.Quiz> getAllQuizzes(Pageable pageable);
    Page<com.vuiquiz.quizwebsocket.model.Quiz> getQuizzesByCreatorId(UUID creatorId, Pageable pageable);
    Page<com.vuiquiz.quizwebsocket.model.Quiz> searchQuizzesByTitle(String title, Pageable pageable);
    Page<com.vuiquiz.quizwebsocket.model.Quiz> getQuizzesByStatus(String status, Pageable pageable);
    Page<com.vuiquiz.quizwebsocket.model.Quiz> getPublicQuizzes(Pageable pageable);
    com.vuiquiz.quizwebsocket.model.Quiz updateQuiz(UUID quizId, com.vuiquiz.quizwebsocket.model.Quiz quizDetails);
    void deleteQuiz(UUID quizId);
    com.vuiquiz.quizwebsocket.model.Quiz updateQuizStatus(UUID quizId, String newStatus);
    com.vuiquiz.quizwebsocket.model.Quiz updateQuizVisibility(UUID quizId, Integer newVisibility);
}