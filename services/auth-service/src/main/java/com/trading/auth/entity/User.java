package com.trading.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * User entity — stored in auth_db.
 * KYC status is mocked — always VERIFIED in dev.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(unique = true, length = 20)
    private String phone;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", length = 255)
    private String fullName;

    @Column(name = "kyc_status", length = 20)
    @Builder.Default
    private String kycStatus = "VERIFIED";   // always VERIFIED in mock mode

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

    @Column(name = "role", length = 20)
    @Builder.Default
    private String role = "ROLE_USER";       // ROLE_USER or ROLE_ADMIN

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
