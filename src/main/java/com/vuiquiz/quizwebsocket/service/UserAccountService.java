package com.vuiquiz.quizwebsocket.service;

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
}
