package com.vuiquiz.quizwebsocket.model;

import lombok.AllArgsConstructor;
import lombok.Builder; // Ensure this import is present
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder // Existing annotation
@Entity
@Table(name = "user_account", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"username", "email"}, name = "AK_user_account_username_email")
})
@SQLDelete(sql = "UPDATE user_account SET deleted_at = NOW() WHERE user_id = ?")
@Where(clause = "deleted_at IS NULL")
public class UserAccount {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID userId;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "email", unique = true, length = 200)
    private String email;

    @Column(name = "account_password", nullable = false, length = 72) // Length was already updated, good.
    private String accountPassword;

    @Column(name = "role", nullable = false, length = 50)
    @Builder.Default // Add this annotation
    private String role = "USER";

    @Column(name = "storage_used", nullable = false)
    @Builder.Default // Add this annotation
    private Long storageUsed = 0L;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime deletedAt;

    @Column(name = "storage_limit", nullable = false)
    @Builder.Default
    private Long storageLimit = 50 * 1024 * 1024L;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
        // storageUsed will be initialized by @Builder.Default or if explicitly set
        // role will be initialized by @Builder.Default or if explicitly set
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}