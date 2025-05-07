package com.vuiquiz.quizwebsocket.service.impl;

import com.vuiquiz.quizwebsocket.exception.ResourceNotFoundException;
import com.vuiquiz.quizwebsocket.model.UserAccount;
import com.vuiquiz.quizwebsocket.repository.UserAccountRepository;
import com.vuiquiz.quizwebsocket.service.UserAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
// import org.springframework.security.crypto.password.PasswordEncoder; // Example for password hashing

import java.util.Optional;
import java.util.UUID;

@Service
public class UserAccountServiceImpl implements UserAccountService {

    private final UserAccountRepository userAccountRepository;
    // private final PasswordEncoder passwordEncoder; // Inject if handling password encoding here

    @Autowired
    public UserAccountServiceImpl(UserAccountRepository userAccountRepository /*, PasswordEncoder passwordEncoder */) {
        this.userAccountRepository = userAccountRepository;
        // this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public UserAccount createUser(UserAccount userAccount) {
        if (userAccountRepository.existsByUsername(userAccount.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + userAccount.getUsername());
        }
        if (userAccount.getEmail() != null && userAccountRepository.existsByEmail(userAccount.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + userAccount.getEmail());
        }
        // userAccount.setAccountPassword(passwordEncoder.encode(userAccount.getAccountPassword())); // Example
        userAccount.setUserId(null);
        return userAccountRepository.save(userAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAccount> getUserById(UUID userId) {
        return userAccountRepository.findById(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAccount> getUserByUsername(String username) {
        return userAccountRepository.findByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAccount> getUserByEmail(String email) {
        return userAccountRepository.findByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserAccount> getAllUsers(Pageable pageable) {
        return userAccountRepository.findAll(pageable);
    }

    @Override
    @Transactional
    public UserAccount updateUser(UUID userId, UserAccount userDetails) {
        UserAccount existingUser = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("UserAccount", "id", userId));

        // Prevent username change or handle carefully if allowed
        // existingUser.setUsername(userDetails.getUsername());
        if (userDetails.getEmail() != null && !userDetails.getEmail().equals(existingUser.getEmail())) {
            if (userAccountRepository.existsByEmail(userDetails.getEmail())) {
                throw new IllegalArgumentException("Email already exists: " + userDetails.getEmail());
            }
            existingUser.setEmail(userDetails.getEmail());
        }
        // existingUser.setRole(userDetails.getRole()); // Prefer specific methods like updateUserRole
        existingUser.setStorageUsed(userDetails.getStorageUsed());
        return userAccountRepository.save(existingUser);
    }

    @Override
    @Transactional
    public void deleteUser(UUID userId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("UserAccount", "id", userId));
        userAccountRepository.delete(user);
    }

    @Override
    @Transactional
    public UserAccount updateUserRole(UUID userId, String newRole) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("UserAccount", "id", userId));
        user.setRole(newRole);
        return userAccountRepository.save(user);
    }

    @Override
    @Transactional
    public UserAccount updateUserPassword(UUID userId, String newPassword) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("UserAccount", "id", userId));
        // user.setAccountPassword(passwordEncoder.encode(newPassword)); // Example
        user.setAccountPassword(newPassword); // Assuming password is already hashed by caller
        return userAccountRepository.save(user);
    }
}
