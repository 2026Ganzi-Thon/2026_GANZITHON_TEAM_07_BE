package com.ganzithon.backend.auth.domain;

public record SocialProfile(String providerId, String email, String nickname, boolean emailVerified) {
}
