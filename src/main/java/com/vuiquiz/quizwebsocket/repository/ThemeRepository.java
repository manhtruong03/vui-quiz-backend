package com.vuiquiz.quizwebsocket.repository;

import com.vuiquiz.quizwebsocket.model.Theme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ThemeRepository extends JpaRepository<Theme, UUID> {
    Optional<Theme> findByName(String name);
    List<Theme> findByIsActive(boolean isActive);
}
