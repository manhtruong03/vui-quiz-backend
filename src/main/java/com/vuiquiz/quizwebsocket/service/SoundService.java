package com.vuiquiz.quizwebsocket.service;

import com.vuiquiz.quizwebsocket.model.Sound;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SoundService {
    Sound createSound(Sound sound);
    Optional<Sound> getSoundById(UUID soundId);
    Page<Sound> getAllSounds(Pageable pageable);
    List<Sound> getActiveSoundsByType(String soundType);
    Sound updateSound(UUID soundId, Sound soundDetails);
    void deleteSound(UUID soundId); // Soft delete
    Sound setActiveStatus(UUID soundId, boolean isActive);
}
