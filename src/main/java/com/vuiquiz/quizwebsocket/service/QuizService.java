package com.vuiquiz.quizwebsocket.service;

import com.vuiquiz.quizwebsocket.dto.QuizDTO;
import com.vuiquiz.quizwebsocket.model.Quiz;
import org.springframework.data.domain.Page; // Import Page
import org.springframework.data.domain.Pageable; // Import Pageable
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface QuizService {
    QuizDTO createQuiz(QuizDTO quizDto, UUID creatorId, Map<String, MultipartFile> imageFiles);
    QuizDTO getQuizDetailsById(UUID quizId); // From Phase 4

    // New method for fetching public quizzes
    Page<QuizDTO> getPublicPublishedQuizzes(Pageable pageable);
    Page<QuizDTO> getQuizzesByCurrentUser(Pageable pageable);

    // Existing methods
    Optional<Quiz> getQuizById(UUID quizId);
    Quiz getQuizById_Original(UUID quizId);
    Page<Quiz> getAllQuizzes(Pageable pageable);
    Page<Quiz> getQuizzesByCreatorId(UUID creatorId, Pageable pageable);
    Page<Quiz> searchQuizzesByTitle(String title, Pageable pageable);
    Page<Quiz> getQuizzesByStatus(String status, Pageable pageable);
    Page<Quiz> getPublicQuizzes(Pageable pageable); // Original - maybe remove if replaced by new one
    Quiz updateQuiz(UUID quizId, Quiz quizDetails);
    public void deleteQuiz(UUID quizId);
    Quiz updateQuizStatus(UUID quizId, String newStatus);
    Quiz updateQuizVisibility(UUID quizId, Integer newVisibility);
}