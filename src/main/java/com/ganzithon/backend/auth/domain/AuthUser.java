package com.ganzithon.backend.auth.domain;

public record AuthUser(
        Long id,
        String email,
        String nickname,
        AuthProvider provider,
        String providerId,
        String passwordHash
) {
}
