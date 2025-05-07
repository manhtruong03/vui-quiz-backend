package com.vuiquiz.quizwebsocket.repository;

import com.vuiquiz.quizwebsocket.model.PowerUp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PowerUpRepository extends JpaRepository<PowerUp, UUID> {
    Optional<PowerUp> findByName(String name);
    List<PowerUp> findByIsActive(boolean isActive);
    List<PowerUp> findByPowerUpType(String powerUpType);
}
