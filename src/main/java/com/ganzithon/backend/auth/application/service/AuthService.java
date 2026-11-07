package com.ganzithon.backend.auth.application.service;

import com.ganzithon.backend.auth.application.port.in.AuthUseCase;
import com.ganzithon.backend.auth.application.port.in.AuthFailure;
import com.ganzithon.backend.auth.application.port.out.AuthTokenIssuer;
import com.ganzithon.backend.auth.application.port.out.AuthTransaction;
import com.ganzithon.backend.auth.application.port.out.AuthUserStore;
import com.ganzithon.backend.auth.application.port.out.PasswordHasher;
import com.ganzithon.backend.auth.application.port.out.RefreshTokenStore;
import com.ganzithon.backend.auth.application.port.out.SocialProfileClient;
import com.ganzithon.backend.auth.application.port.out.StarterMalangiStore;
import com.ganzithon.backend.auth.domain.AuthProvider;
import com.ganzithon.backend.auth.domain.AuthUser;
import com.ganzithon.backend.auth.domain.SocialProfile;
import com.ganzithon.backend.auth.domain.StoredRefreshToken;
import com.ganzithon.backend.auth.domain.TokenPair;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;

public class AuthService implements AuthUseCase {
    private final AuthUserStore users;
    private final RefreshTokenStore refreshTokens;
    private final PasswordHasher passwordHasher;
    private final AuthTokenIssuer tokenIssuer;
    private final SocialProfileClient socialProfileClient;
    private final StarterMalangiStore starterMalangiStore;
    private final AuthTransaction transaction;

    public AuthService(
            AuthUserStore users,
            RefreshTokenStore refreshTokens,
            PasswordHasher passwordHasher,
            AuthTokenIssuer tokenIssuer,
            SocialProfileClient socialProfileClient,
            StarterMalangiStore starterMalangiStore,
            AuthTransaction transaction
    ) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
        this.socialProfileClient = socialProfileClient;
        this.starterMalangiStore = starterMalangiStore;
        this.transaction = transaction;
    }

    @Override
    public AuthUser signup(SignupCommand command) {
        String email = normalizeEmail(command.email());
        validatePassword(command.password());
        String passwordHash = passwordHasher.hash(command.password());
        return transaction.required(() -> {
            rejectExistingEmail(email);
            return users.save(new AuthUser(null, email, command.nickname().trim(), AuthProvider.LOCAL, null, passwordHash));
        });
    }

    @Override
    public AuthenticatedUser login(LoginCommand command) {
        String email = normalizeEmail(command.email());
        return transaction.required(() -> {
            AuthUser user = users.findByEmail(email).orElseThrow(this::invalidCredentials);
            if (user.provider() != AuthProvider.LOCAL || user.passwordHash() == null
                    || !passwordHasher.matches(command.password(), user.passwordHash())) {
                throw invalidCredentials();
            }
            return authenticated(user, false);
        });
    }

    @Override
    public AuthenticatedUser socialLogin(SocialLoginCommand command) {
        if (command.provider() == AuthProvider.LOCAL) {
            throw new AuthFailure(AuthFailure.Code.INVALID_REQUEST);
        }
        SocialProfile socialProfile = socialProfileClient.exchange(
                command.provider(), command.authorizationCode(), command.redirectUri());
        if (socialProfile == null || !socialProfile.emailVerified()
                || isBlank(socialProfile.providerId()) || isBlank(socialProfile.email())
                || isBlank(socialProfile.nickname())) {
            throw new AuthFailure(AuthFailure.Code.INVALID_SOCIAL_CREDENTIALS);
        }
        String email = normalizeEmail(socialProfile.email());

        return transaction.required(() -> {
            var existingIdentity = users.findByProviderId(command.provider(), socialProfile.providerId());
            if (existingIdentity.isPresent()) {
                return authenticated(existingIdentity.get(), false);
            }

            var existingEmail = users.findByEmail(email);
            if (existingEmail.isPresent()) {
                AuthUser existingUser = existingEmail.get();
                throw new AuthFailure(AuthFailure.Code.EMAIL_ALREADY_REGISTERED, existingUser.provider(), email);
            }

            AuthUser newUser = users.save(new AuthUser(
                    null, email, socialProfile.nickname().trim(), command.provider(), socialProfile.providerId(), null));
            starterMalangiStore.grantSlangi(newUser.id());
            return authenticated(newUser, true);
        });
    }

    @Override
    public TokenPair refresh(String rawRefreshToken) {
        long userId = tokenIssuer.refreshTokenSubject(rawRefreshToken)
                .orElseThrow(() -> new AuthFailure(AuthFailure.Code.INVALID_REFRESH_TOKEN));
        String tokenHash = TokenHash.sha256(rawRefreshToken);
        return transaction.required(() -> {
            StoredRefreshToken oldToken = refreshTokens.findByHashForUpdate(tokenHash)
                    .filter(token -> !token.revoked() && token.userId() == userId && token.expiresAt().isAfter(Instant.now()))
                    .orElseThrow(() -> new AuthFailure(AuthFailure.Code.INVALID_REFRESH_TOKEN));
            AuthUser user = users.findById(userId)
                    .orElseThrow(() -> new AuthFailure(AuthFailure.Code.INVALID_REFRESH_TOKEN));
            refreshTokens.revoke(oldToken.id());
            return issueAndStore(user);
        });
    }

    @Override
    public void logout(long userId, String rawRefreshToken) {
        transaction.required(() -> {
            StoredRefreshToken token = refreshTokens.findByHashForUpdate(TokenHash.sha256(rawRefreshToken))
                    .filter(stored -> !stored.revoked() && stored.userId() == userId)
                    .orElseThrow(() -> new AuthFailure(AuthFailure.Code.INVALID_REFRESH_TOKEN));
            refreshTokens.revoke(token.id());
            return null;
        });
    }

    @Override
    public AuthUser profile(long userId) {
        return users.findById(userId).orElseThrow(() -> new AuthFailure(AuthFailure.Code.USER_NOT_FOUND));
    }

    private AuthenticatedUser authenticated(AuthUser user, boolean newUser) {
        return new AuthenticatedUser(user, issueAndStore(user), newUser);
    }

    private TokenPair issueAndStore(AuthUser user) {
        TokenPair tokens = tokenIssuer.issue(user);
        refreshTokens.save(user.id(), TokenHash.sha256(tokens.refreshToken()), tokens.refreshExpiresAt());
        return tokens;
    }

    private void rejectExistingEmail(String email) {
        users.findByEmail(email).ifPresent(user -> {
            throw new AuthFailure(AuthFailure.Code.EMAIL_ALREADY_REGISTERED, user.provider(), email);
        });
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void validatePassword(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length < 8 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new AuthFailure(AuthFailure.Code.INVALID_REQUEST);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private AuthFailure invalidCredentials() {
        return new AuthFailure(AuthFailure.Code.INVALID_CREDENTIALS);
    }
}
