package com.vuiquiz.quizwebsocket.service;

import com.vuiquiz.quizwebsocket.model.QuizTag;

import java.util.List;
import java.util.UUID;

public interface QuizTagService {
    QuizTag addTagToQuiz(UUID quizId, UUID tagId);
    void removeTagFromQuiz(UUID quizId, UUID tagId);
    List<QuizTag> getTagsByQuizId(UUID quizId);
    List<QuizTag> getQuizzesByTagId(UUID tagId);
    boolean isTagAssociatedWithQuiz(UUID quizId, UUID tagId);
}
