package com.vuiquiz.quizwebsocket.service.impl;

import com.vuiquiz.quizwebsocket.exception.ResourceNotFoundException;
import com.vuiquiz.quizwebsocket.model.QuizTag;
import com.vuiquiz.quizwebsocket.repository.QuizRepository;
import com.vuiquiz.quizwebsocket.repository.QuizTagRepository;
import com.vuiquiz.quizwebsocket.repository.TagRepository;
import com.vuiquiz.quizwebsocket.service.QuizTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor // Lombok constructor injection
public class QuizTagServiceImpl implements QuizTagService {

    private final QuizTagRepository quizTagRepository;
    // Inject QuizRepository and TagRepository if needed for validation
    private final QuizRepository quizRepository;
    private final TagRepository tagRepository;


    @Override
    @Transactional
    public QuizTag addTagToQuiz(UUID quizId, UUID tagId) {
        // Optional: Validate that quizId and tagId exist
        if (!quizRepository.existsById(quizId)) {
            throw new ResourceNotFoundException("Quiz", "id", quizId);
        }
        if (!tagRepository.existsById(tagId)) {
            throw new ResourceNotFoundException("Tag", "id", tagId);
        }

        // Check if association already exists
        Optional<QuizTag> existingAssociation = quizTagRepository.findByQuizIdAndTagId(quizId, tagId);
        if (existingAssociation.isPresent()) {
            return existingAssociation.get(); // Already associated
        } else {
            QuizTag newAssociation = QuizTag.builder()
                    .quizId(quizId)
                    .tagId(tagId)
                    .build();
            return quizTagRepository.save(newAssociation);
        }
    }

    @Override
    @Transactional
    public void removeTagFromQuiz(UUID quizId, UUID tagId) {
        // Find the specific association
        Optional<QuizTag> association = quizTagRepository.findByQuizIdAndTagId(quizId, tagId);
        // Delete it if it exists
        association.ifPresent(quizTagRepository::delete);
        // Or use the deleteBy method if defined: quizTagRepository.deleteByQuizIdAndTagId(quizId, tagId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizTag> getTagsByQuizId(UUID quizId) {
        return quizTagRepository.findByQuizId(quizId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizTag> getQuizzesByTagId(UUID tagId) {
        return quizTagRepository.findByTagId(tagId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isTagAssociatedWithQuiz(UUID quizId, UUID tagId) {
        return quizTagRepository.findByQuizIdAndTagId(quizId, tagId).isPresent();
    }
}