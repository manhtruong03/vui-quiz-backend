package com.vuiquiz.quizwebsocket.service.impl;

import com.vuiquiz.quizwebsocket.exception.ResourceNotFoundException;
import com.vuiquiz.quizwebsocket.model.Avatar;
import com.vuiquiz.quizwebsocket.repository.AvatarRepository;
import com.vuiquiz.quizwebsocket.service.AvatarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AvatarServiceImpl implements AvatarService {

    private final AvatarRepository avatarRepository;

    @Autowired
    public AvatarServiceImpl(AvatarRepository avatarRepository) {
        this.avatarRepository = avatarRepository;
    }

    @Override
    @Transactional
    public Avatar createAvatar(Avatar avatar) {
        avatar.setAvatarId(null);
        return avatarRepository.save(avatar);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Avatar> getAvatarById(UUID avatarId) {
        return avatarRepository.findById(avatarId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Avatar> getAllAvatars(Pageable pageable) {
        return avatarRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Avatar> getActiveAvatars() {
        return avatarRepository.findByIsActive(true);
    }

    @Override
    @Transactional
    public Avatar updateAvatar(UUID avatarId, Avatar avatarDetails) {
        Avatar existingAvatar = avatarRepository.findById(avatarId)
                .orElseThrow(() -> new ResourceNotFoundException("Avatar", "id", avatarId));

        existingAvatar.setName(avatarDetails.getName());
        existingAvatar.setDescription(avatarDetails.getDescription());
        existingAvatar.setImageFilePath(avatarDetails.getImageFilePath());
        existingAvatar.setActive(avatarDetails.isActive());
        return avatarRepository.save(existingAvatar);
    }

    @Override
    @Transactional
    public void deleteAvatar(UUID avatarId) {
        Avatar avatar = avatarRepository.findById(avatarId)
                .orElseThrow(() -> new ResourceNotFoundException("Avatar", "id", avatarId));
        avatarRepository.delete(avatar);
    }

    @Override
    @Transactional
    public Avatar setActiveStatus(UUID avatarId, boolean isActive) {
        Avatar avatar = avatarRepository.findById(avatarId)
                .orElseThrow(() -> new ResourceNotFoundException("Avatar", "id", avatarId));
        avatar.setActive(isActive);
        return avatarRepository.save(avatar);
    }
}