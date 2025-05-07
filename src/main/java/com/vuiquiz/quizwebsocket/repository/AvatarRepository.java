package com.vuiquiz.quizwebsocket.repository;

import com.vuiquiz.quizwebsocket.model.Avatar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AvatarRepository extends JpaRepository<Avatar, UUID> {
    Optional<Avatar> findByName(String name);
    List<Avatar> findByIsActive(boolean isActive);
}
