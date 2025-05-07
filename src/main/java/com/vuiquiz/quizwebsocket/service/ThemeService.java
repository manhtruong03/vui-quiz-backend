package com.vuiquiz.quizwebsocket.service;

import com.vuiquiz.quizwebsocket.model.Theme;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ThemeService {
    Theme createTheme(Theme theme);
    Optional<Theme> getThemeById(UUID themeId);
    Optional<Theme> getThemeByName(String name);
    List<Theme> getAllThemes();
    Page<Theme> getAllThemes(Pageable pageable);
    List<Theme> getActiveThemes();
    Theme updateTheme(UUID themeId, Theme themeDetails);
    void deleteTheme(UUID themeId);
    Theme setActiveStatus(UUID themeId, boolean isActive);
}
