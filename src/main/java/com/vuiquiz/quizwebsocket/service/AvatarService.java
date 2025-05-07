package com.vuiquiz.quizwebsocket.service;

import com.vuiquiz.quizwebsocket.model.Avatar;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AvatarService {
    Avatar createAvatar(Avatar avatar);
    Optional<Avatar> getAvatarById(UUID avatarId);
    Page<Avatar> getAllAvatars(Pageable pageable);
    List<Avatar> getActiveAvatars();
    Avatar updateAvatar(UUID avatarId, Avatar avatarDetails);
    void deleteAvatar(UUID avatarId);
    Avatar setActiveStatus(UUID avatarId, boolean isActive);
}
