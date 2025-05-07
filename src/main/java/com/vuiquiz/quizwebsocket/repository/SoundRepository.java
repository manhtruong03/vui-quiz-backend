package com.vuiquiz.quizwebsocket.repository;

import com.vuiquiz.quizwebsocket.model.Sound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SoundRepository extends JpaRepository<Sound, UUID> {
    Optional<Sound> findByName(String name);
    List<Sound> findByIsActive(boolean isActive);
    List<Sound> findBySoundTypeAndIsActive(String soundType, boolean isActive);
}
