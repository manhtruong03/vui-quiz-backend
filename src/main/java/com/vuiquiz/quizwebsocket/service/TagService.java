package com.vuiquiz.quizwebsocket.service;

import com.vuiquiz.quizwebsocket.dto.TagAdminViewDTO;
import com.vuiquiz.quizwebsocket.dto.TagCreationRequestDTO;
import com.vuiquiz.quizwebsocket.dto.TagUpdateRequestDTO;
import com.vuiquiz.quizwebsocket.model.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TagService {
    Tag createTag(Tag tag);
    Optional<Tag> getTagById(UUID tagId);
    Optional<Tag> getTagByName(String name);
    Tag findOrCreateTagByName(String name); // Useful helper
    Page<Tag> getAllTags(Pageable pageable);
    List<Tag> searchTagsByName(String namePart);
    Tag updateTag(UUID tagId, Tag tagDetails);
    void deleteTag(UUID tagId); // Soft delete

    TagAdminViewDTO adminCreateTag(TagCreationRequestDTO creationRequest);
    Page<TagAdminViewDTO> adminGetAllTags(Pageable pageable);
    TagAdminViewDTO adminGetTagById(UUID tagId);
    TagAdminViewDTO adminUpdateTag(UUID tagId, TagUpdateRequestDTO updateRequest);
}
