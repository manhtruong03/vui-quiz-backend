package com.vuiquiz.quizwebsocket.service;

import com.vuiquiz.quizwebsocket.model.PowerUp;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PowerUpService {
    PowerUp createPowerUp(PowerUp powerUp);
    Optional<PowerUp> getPowerUpById(UUID powerUpId);
    Optional<PowerUp> getPowerUpByName(String name);
    Page<PowerUp> getAllPowerUps(Pageable pageable);
    List<PowerUp> getActivePowerUps();
    PowerUp updatePowerUp(UUID powerUpId, PowerUp powerUpDetails);
    void deletePowerUp(UUID powerUpId); // Soft delete
    PowerUp setActiveStatus(UUID powerUpId, boolean isActive);
}
