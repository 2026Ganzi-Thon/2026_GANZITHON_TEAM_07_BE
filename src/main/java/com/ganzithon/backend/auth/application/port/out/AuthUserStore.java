package com.ganzithon.backend.auth.application.port.out;

import com.ganzithon.backend.auth.domain.AuthProvider;
import com.ganzithon.backend.auth.domain.AuthUser;
import java.util.Optional;

public interface AuthUserStore {
    Optional<AuthUser> findByEmail(String email);

    Optional<AuthUser> findById(long userId);

    Optional<AuthUser> findByProviderId(AuthProvider provider, String providerId);

    AuthUser save(AuthUser user);
}
