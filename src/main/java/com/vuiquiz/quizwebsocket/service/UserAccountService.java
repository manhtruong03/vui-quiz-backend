package com.vuiquiz.quizwebsocket.service;

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
}
