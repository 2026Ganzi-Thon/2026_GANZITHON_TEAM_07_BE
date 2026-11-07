package com.ganzithon.backend.auth.application.port.in;

import com.ganzithon.backend.auth.domain.AuthProvider;
import com.ganzithon.backend.auth.domain.AuthUser;
import com.ganzithon.backend.auth.domain.TokenPair;

public interface AuthUseCase {
    AuthUser signup(SignupCommand command);

    AuthenticatedUser login(LoginCommand command);

    AuthenticatedUser socialLogin(SocialLoginCommand command);

    TokenPair refresh(String refreshToken);

    void logout(long userId, String refreshToken);

    AuthUser profile(long userId);

    record SignupCommand(String email, String password, String nickname) {}

    record LoginCommand(String email, String password) {}

    record SocialLoginCommand(AuthProvider provider, String authorizationCode, String redirectUri) {}

    record AuthenticatedUser(AuthUser user, TokenPair tokens, boolean newUser) {}
}
