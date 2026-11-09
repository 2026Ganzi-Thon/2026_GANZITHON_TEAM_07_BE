package com.ganzithon.backend.auth.adapter.out.persistence;

import com.ganzithon.backend.auth.domain.AuthProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_users_provider_id", columnNames = {"provider", "provider_id"})
})
class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthProvider provider;

    @Column(name = "provider_id", length = 255)
    private String providerId;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected UserEntity() {}

    UserEntity(String email, String passwordHash, String nickname, AuthProvider provider, String providerId) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.provider = provider;
        this.providerId = providerId;
    }

    Long id() { return id; }
    String email() { return email; }
    String passwordHash() { return passwordHash; }
    String nickname() { return nickname; }
    AuthProvider provider() { return provider; }
    String providerId() { return providerId; }
}
