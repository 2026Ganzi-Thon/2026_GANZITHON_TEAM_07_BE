package com.ganzithon.backend.auth.adapter.out.security;

import com.ganzithon.backend.auth.application.port.out.AuthTokenIssuer;
import com.ganzithon.backend.auth.domain.AuthUser;
import com.ganzithon.backend.auth.domain.TokenPair;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
class JwtTokenAdapter implements AuthTokenIssuer {
    private static final Duration ACCESS_TOKEN_LIFETIME = Duration.ofHours(24);
    private static final Duration REFRESH_TOKEN_LIFETIME = Duration.ofDays(14);
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final Clock clock;

    JwtTokenAdapter(JwtEncoder encoder, @Qualifier("refreshJwtDecoder") JwtDecoder decoder, Clock clock) {
        this.encoder = encoder;
        this.decoder = decoder;
        this.clock = clock;
    }

    @Override
    public TokenPair issue(AuthUser user) {
        Instant now = clock.instant();
        String accessToken = encode(user, "access", now, now.plus(ACCESS_TOKEN_LIFETIME));
        Instant refreshExpiresAt = now.plus(REFRESH_TOKEN_LIFETIME);
        String refreshToken = encode(user, "refresh", now, refreshExpiresAt);
        return new TokenPair(accessToken, refreshToken, ACCESS_TOKEN_LIFETIME.toSeconds(), refreshExpiresAt);
    }

    @Override
    public Optional<Long> refreshTokenSubject(String refreshToken) {
        try {
            Jwt jwt = decoder.decode(refreshToken);
            if (!"refresh".equals(jwt.getClaimAsString("tokenType"))) {
                return Optional.empty();
            }
            return Optional.of(Long.parseLong(jwt.getSubject()));
        } catch (JwtException | NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private String encode(AuthUser user, String tokenType, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("ganzithon")
                .subject(user.id().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim("email", user.email())
                .claim("nickname", user.nickname())
                .claim("provider", user.provider().name())
                .claim("tokenType", tokenType)
                .build();
        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(), claims)).getTokenValue();
    }
}
