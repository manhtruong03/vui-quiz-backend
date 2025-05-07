package com.vuiquiz.quizwebsocket.repository;

import com.vuiquiz.quizwebsocket.model.Quiz;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, UUID> {
    List<Quiz> findByCreatorId(UUID creatorId);
    Page<Quiz> findByCreatorId(UUID creatorId, Pageable pageable);
    Page<Quiz> findByTitleContainingIgnoreCase(String title, Pageable pageable);
    Page<Quiz> findByStatus(String status, Pageable pageable);
    Page<Quiz> findByVisibility(Integer visibility, Pageable pageable);

    @Query("SELECT q FROM Quiz q WHERE q.visibility = 1 AND q.status = 'PUBLISHED'") // Public and Published
    Page<Quiz> findPublicAndPublished(Pageable pageable);
    Page<Quiz> findByVisibilityAndStatus(Integer visibility, String status, Pageable pageable);
}
