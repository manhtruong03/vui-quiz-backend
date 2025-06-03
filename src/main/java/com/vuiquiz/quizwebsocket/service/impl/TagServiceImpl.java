package com.vuiquiz.quizwebsocket.service.impl;

import com.vuiquiz.quizwebsocket.dto.TagAdminViewDTO;
import com.vuiquiz.quizwebsocket.dto.TagCreationRequestDTO;
import com.vuiquiz.quizwebsocket.dto.TagUpdateRequestDTO;
import com.vuiquiz.quizwebsocket.exception.ResourceNotFoundException;
import com.vuiquiz.quizwebsocket.model.Tag;
import com.vuiquiz.quizwebsocket.repository.TagRepository;
import com.vuiquiz.quizwebsocket.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor // Lombok constructor injection
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;

    // Helper method to map Tag entity to TagAdminViewDTO
    private TagAdminViewDTO mapTagToTagAdminViewDTO(Tag tag) {
        if (tag == null) {
            return null;
        }
        return TagAdminViewDTO.builder()
                .tagId(tag.getTagId())
                .name(tag.getName())
                .description(tag.getDescription())
                .createdAt(tag.getCreatedAt())
                .updatedAt(tag.getUpdatedAt())
                .build();
    }


    @Override
    @Transactional
    public Tag createTag(Tag tag) {
        // Basic validation or normalization could happen here (e.g., lowercase name)
        if (tagRepository.findByName(tag.getName()).isPresent()) {
            throw new IllegalArgumentException("Tag with name '" + tag.getName() + "' already exists.");
        }
        tag.setTagId(null); // Ensure ID is generated
        return tagRepository.save(tag);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Tag> getTagById(UUID tagId) {
        return tagRepository.findById(tagId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Tag> getTagByName(String name) {
        return tagRepository.findByName(name);
    }

    @Override
    @Transactional
    public Tag findOrCreateTagByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Tag name cannot be empty.");
        }
        // Consider normalizing the tag name (e.g., lowercase, trim)
        String normalizedName = name.trim(); //.toLowerCase(); // Optional: make tags case-insensitive

        return tagRepository.findByName(normalizedName)
                .orElseGet(() -> {
                    Tag newTag = Tag.builder()
                            .name(normalizedName)
                            // Add description if needed/possible, or leave null
                            .build();
                    return tagRepository.save(newTag);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Tag> getAllTags(Pageable pageable) {
        return tagRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tag> searchTagsByName(String namePart) {
        return tagRepository.findByNameContainingIgnoreCase(namePart);
    }

    @Override
    @Transactional
    public Tag updateTag(UUID tagId, Tag tagDetails) {
        Tag existingTag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag", "id", tagId));

        // Prevent changing name to one that already exists (unless it's the same tag)
        Optional<Tag> tagWithNewName = tagRepository.findByName(tagDetails.getName());
        if (tagWithNewName.isPresent() && !tagWithNewName.get().getTagId().equals(tagId)) {
            throw new IllegalArgumentException("Tag with name '" + tagDetails.getName() + "' already exists.");
        }

        existingTag.setName(tagDetails.getName());
        existingTag.setDescription(tagDetails.getDescription());
        return tagRepository.save(existingTag);
    }

    @Override
    @Transactional
    public void deleteTag(UUID tagId) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag", "id", tagId));
        // Consider implications: deleting a tag might require removing related QuizTag entries first
        // depending on DB constraints or desired behavior. Add logic here if needed.
        // e.g., quizTagRepository.deleteByTagId(tagId);
        tagRepository.delete(tag);
    }

    @Override
    @Transactional
    public TagAdminViewDTO adminCreateTag(TagCreationRequestDTO creationRequest) {
        // Normalize and validate name
        String tagName = creationRequest.getName().trim();
        if (tagRepository.findByName(tagName).isPresent()) {
            throw new IllegalArgumentException("Error: Tag name '" + tagName + "' already exists.");
        }

        Tag newTag = Tag.builder()
                .name(tagName)
                .description(creationRequest.getDescription())
                .build();
        // @PrePersist in Tag entity handles createdAt and updatedAt
        Tag savedTag = tagRepository.save(newTag);
        return mapTagToTagAdminViewDTO(savedTag);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TagAdminViewDTO> adminGetAllTags(Pageable pageable) {
        Page<Tag> tagPage = tagRepository.findAll(pageable); // This already respects @Where(clause = "deleted_at IS NULL")
        return tagPage.map(this::mapTagToTagAdminViewDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public TagAdminViewDTO adminGetTagById(UUID tagId) {
        Tag tag = tagRepository.findById(tagId) // Respects @Where for soft delete
                .orElseThrow(() -> new ResourceNotFoundException("Tag", "id", tagId));
        return mapTagToTagAdminViewDTO(tag);
    }

    @Override
    @Transactional
    public TagAdminViewDTO adminUpdateTag(UUID tagId, TagUpdateRequestDTO updateRequest) {
        Tag existingTag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag", "id", tagId));

        boolean updated = false;

        if (StringUtils.hasText(updateRequest.getName())) {
            String newName = updateRequest.getName().trim();
            if (!newName.equalsIgnoreCase(existingTag.getName())) { // Case-insensitive check for changes
                Optional<Tag> tagWithNewName = tagRepository.findByName(newName);
                if (tagWithNewName.isPresent() && !tagWithNewName.get().getTagId().equals(tagId)) {
                    throw new IllegalArgumentException("Error: Tag name '" + newName + "' already exists.");
                }
                existingTag.setName(newName);
                updated = true;
            }
        }

        if (updateRequest.getDescription() != null) { // Allow setting description to empty or null
            if (!Objects.equals(updateRequest.getDescription(), existingTag.getDescription())) {
                existingTag.setDescription(StringUtils.hasText(updateRequest.getDescription()) ? updateRequest.getDescription() : null);
                updated = true;
            }
        }

        if (updated) {
            // @PreUpdate in Tag entity handles updatedAt
            Tag savedTag = tagRepository.save(existingTag);
            return mapTagToTagAdminViewDTO(savedTag);
        }
        return mapTagToTagAdminViewDTO(existingTag); // No changes, return current state
    }
}