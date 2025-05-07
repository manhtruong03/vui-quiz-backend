package com.vuiquiz.quizwebsocket.service.impl;

import com.vuiquiz.quizwebsocket.exception.ResourceNotFoundException;
import com.vuiquiz.quizwebsocket.model.Theme;
import com.vuiquiz.quizwebsocket.repository.ThemeRepository;
import com.vuiquiz.quizwebsocket.service.ThemeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ThemeServiceImpl implements ThemeService {

    private final ThemeRepository themeRepository;

    @Autowired
    public ThemeServiceImpl(ThemeRepository themeRepository) {
        this.themeRepository = themeRepository;
    }

    @Override
    @Transactional
    public Theme createTheme(Theme theme) {
        // Ensure no ID is set, createdAt and updatedAt are handled by @PrePersist
        theme.setThemeId(null);
        return themeRepository.save(theme);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Theme> getThemeById(UUID themeId) {
        return themeRepository.findById(themeId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Theme> getThemeByName(String name) {
        return themeRepository.findByName(name);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Theme> getAllThemes() {
        return themeRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Theme> getAllThemes(Pageable pageable) {
        return themeRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Theme> getActiveThemes() {
        return themeRepository.findByIsActive(true);
    }

    @Override
    @Transactional
    public Theme updateTheme(UUID themeId, Theme themeDetails) {
        Theme existingTheme = themeRepository.findById(themeId)
                .orElseThrow(() -> new ResourceNotFoundException("Theme", "id", themeId));

        existingTheme.setName(themeDetails.getName());
        existingTheme.setDescription(themeDetails.getDescription());
        existingTheme.setBackgroundFilePath(themeDetails.getBackgroundFilePath());
        existingTheme.setBackgroundColor(themeDetails.getBackgroundColor());
        existingTheme.setTextColor(themeDetails.getTextColor());
        existingTheme.setActive(themeDetails.isActive());
        // updatedAt is handled by @PreUpdate
        return themeRepository.save(existingTheme);
    }

    @Override
    @Transactional
    public void deleteTheme(UUID themeId) {
        Theme theme = themeRepository.findById(themeId)
                .orElseThrow(() -> new ResourceNotFoundException("Theme", "id", themeId));
        themeRepository.delete(theme); // Will trigger soft delete if @SQLDelete is configured
    }

    @Override
    @Transactional
    public Theme setActiveStatus(UUID themeId, boolean isActive) {
        Theme theme = themeRepository.findById(themeId)
                .orElseThrow(() -> new ResourceNotFoundException("Theme", "id", themeId));
        theme.setActive(isActive);
        return themeRepository.save(theme);
    }
}
