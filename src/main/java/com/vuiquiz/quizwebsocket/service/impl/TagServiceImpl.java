package com.vuiquiz.quizwebsocket.service.impl;

import com.vuiquiz.quizwebsocket.exception.ResourceNotFoundException;
import com.vuiquiz.quizwebsocket.model.Tag;
import com.vuiquiz.quizwebsocket.repository.TagRepository;
import com.vuiquiz.quizwebsocket.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor // Lombok constructor injection
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;

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
}