package com.vuiquiz.quizwebsocket.service;

import com.vuiquiz.quizwebsocket.model.Quiz;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface QuizService {
    Quiz createQuiz(Quiz quiz);
    Optional<Quiz> getQuizById(UUID quizId);
    Page<Quiz> getAllQuizzes(Pageable pageable);
    Page<Quiz> getQuizzesByCreatorId(UUID creatorId, Pageable pageable);
    Page<Quiz> searchQuizzesByTitle(String title, Pageable pageable);
    Page<Quiz> getQuizzesByStatus(String status, Pageable pageable);
    Page<Quiz> getPublicQuizzes(Pageable pageable); // Example: visibility = 1 and status = PUBLISHED
    Quiz updateQuiz(UUID quizId, Quiz quizDetails);
    void deleteQuiz(UUID quizId);
    Quiz updateQuizStatus(UUID quizId, String newStatus);
    Quiz updateQuizVisibility(UUID quizId, Integer newVisibility);
    // Methods to manage questions (e.g., QuestionService.findByQuizId()) will be separate
    // Methods to manage tags (e.g., QuizTagService) will be separate
}