package com.ganzithon.backend.auth.domain;

import java.time.Instant;

public record TokenPair(String accessToken, String refreshToken, long expiresIn, Instant refreshExpiresAt) {
}
