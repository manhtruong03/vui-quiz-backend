package com.vuiquiz.quizwebsocket.service;


import com.vuiquiz.quizwebsocket.model.Question;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuestionService {
    Question createQuestion(Question question); // Requires valid quizId, imageId (optional)
    Optional<Question> getQuestionById(UUID questionId);
    List<Question> getQuestionsByQuizId(UUID quizId); // Ordered by position
    Question updateQuestion(UUID questionId, Question questionDetails);
    void deleteQuestion(UUID questionId); // Soft delete
    void deleteQuestionsByQuizId(UUID quizId); // For bulk deletion when a quiz is deleted
    // Method to reorder questions within a quiz might be needed
}