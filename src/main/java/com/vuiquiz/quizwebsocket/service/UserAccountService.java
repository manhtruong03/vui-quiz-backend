package com.vuiquiz.quizwebsocket.service;

import com.vuiquiz.quizwebsocket.dto.UserAccountAdminViewDTO;
import com.vuiquiz.quizwebsocket.dto.UserAccountCreationRequestDTO;
import com.vuiquiz.quizwebsocket.dto.UserAccountUpdateRequestDTO;
import com.vuiquiz.quizwebsocket.exception.ResourceNotFoundException;
import com.vuiquiz.quizwebsocket.model.UserAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountService {
    UserAccount createUser(UserAccount userAccount); // Password should be hashed before this call
    Optional<UserAccount> getUserById(UUID userId);
    Optional<UserAccount> getUserByUsername(String username);
    Optional<UserAccount> getUserByEmail(String email);
    Page<UserAccount> getAllUsers(Pageable pageable);
    UserAccount updateUser(UUID userId, UserAccount userDetails);
    void deleteUser(UUID userId);
    // Add methods for password change, role update, storage update etc. as needed
    UserAccount updateUserRole(UUID userId, String newRole);
    UserAccount updateUserPassword(UUID userId, String newPassword); // Ensure newPassword is hashed

    /**
     * Checks if a user can upload a file of a given size based on their storage quota.
     * @param userId The ID of the user.
     * @param fileSize The size of the file to be uploaded (in bytes).
     * @return true if the user has enough storage, false otherwise.
     * @throws ResourceNotFoundException if user not found.
     */
    boolean canUserUpload(UUID userId, long fileSize);

    /**
     * Updates the storage used by a user.
     * @param userId The ID of the user.
     * @param fileSizeDelta The change in file size (positive for addition, negative for deletion).
     * @return The updated UserAccount.
     * @throws ResourceNotFoundException if user not found.
     * @throws IllegalArgumentException if updating would result in negative storageUsed.
     */
    UserAccount updateUserStorageUsed(UUID userId, long fileSizeDelta);

    /**
     * Creates a new user account by an administrator.
     * @param creationRequest DTO containing new user details.
     * @return UserAccountAdminViewDTO of the created user.
     * @throws IllegalArgumentException if username/email already exists or role is invalid.
     */
    UserAccountAdminViewDTO createUserByAdmin(UserAccountCreationRequestDTO creationRequest);

    /**
     * Updates an existing user account by an administrator.
     * Only non-null fields in the updateRequest will be considered for update.
     * @param userId The UUID of the user to update.
     * @param updateRequest DTO containing the fields to update.
     * @return UserAccountAdminViewDTO of the updated user.
     * @throws ResourceNotFoundException if the user is not found.
     * @throws IllegalArgumentException if new username/email conflicts with an existing one, or role is invalid.
     */
    UserAccountAdminViewDTO updateUserByAdmin(UUID userId, UserAccountUpdateRequestDTO updateRequest);

    /**
     * Sets or resets a user's password by an administrator.
     * The new password will be encoded before being saved.
     * @param userId The UUID of the user whose password is to be changed.
     * @param newPassword The new plain text password.
     * @throws ResourceNotFoundException if the user is not found.
     */
    void adminSetUserPassword(UUID userId, String newPassword);

    /**
     * Soft deletes a user account by an administrator.
     * The user account will be marked as deleted and will not be retrievable by standard queries.
     * @param userId The UUID of the user to soft delete.
     * @throws ResourceNotFoundException if the user is not found (or already soft-deleted).
     */
    void deleteUserByAdmin(UUID userId);
}
