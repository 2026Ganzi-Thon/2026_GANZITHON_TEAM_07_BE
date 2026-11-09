package com.ganzithon.backend.auth.domain;

import java.time.Instant;

public record StoredRefreshToken(Long id, Long userId, String tokenHash, Instant expiresAt, boolean revoked) {
}
