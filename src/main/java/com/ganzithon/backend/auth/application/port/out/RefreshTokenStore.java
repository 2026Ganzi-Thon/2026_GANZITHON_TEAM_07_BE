package com.ganzithon.backend.auth.application.port.out;

import com.ganzithon.backend.auth.domain.StoredRefreshToken;
import java.util.Optional;

public interface RefreshTokenStore {
    StoredRefreshToken save(long userId, String tokenHash, java.time.Instant expiresAt);

    Optional<StoredRefreshToken> findByHash(String tokenHash);

    Optional<StoredRefreshToken> findByHashForUpdate(String tokenHash);

    void revoke(long tokenId);
}
