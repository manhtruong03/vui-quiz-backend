package com.vuiquiz.quizwebsocket.repository;

import com.vuiquiz.quizwebsocket.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface TagRepository extends JpaRepository<Tag, UUID> {
    Optional<Tag> findByName(String name);
    List<Tag> findByNameContainingIgnoreCase(String namePart);
}
