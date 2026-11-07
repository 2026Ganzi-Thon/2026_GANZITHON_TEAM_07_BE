package com.ganzithon.backend.auth.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "refresh_tokens", uniqueConstraints =
        @UniqueConstraint(name = "uk_refresh_tokens_hash", columnNames = "token_hash"))
class RefreshTokenEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token_hash", nullable = false, length = 255)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "is_revoked", nullable = false)
    private boolean revoked;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected RefreshTokenEntity() {}

    RefreshTokenEntity(long userId, String tokenHash, Instant expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = LocalDateTime.ofInstant(expiresAt, ZoneOffset.UTC);
    }

    Long id() { return id; }
    Long userId() { return userId; }
    String tokenHash() { return tokenHash; }
    Instant expiresAt() { return expiresAt.toInstant(ZoneOffset.UTC); }
    boolean revoked() { return revoked; }
    void revoke() { this.revoked = true; }
}
