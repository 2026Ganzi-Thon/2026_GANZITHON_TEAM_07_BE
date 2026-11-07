package com.ganzithon.backend.auth.application.port.out;

import com.ganzithon.backend.auth.domain.AuthUser;
import com.ganzithon.backend.auth.domain.TokenPair;
import java.util.Optional;

public interface AuthTokenIssuer {
    TokenPair issue(AuthUser user);

    Optional<Long> refreshTokenSubject(String refreshToken);
}
