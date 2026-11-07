package com.ganzithon.backend.auth.adapter.out.persistence;

import com.ganzithon.backend.auth.application.port.out.RefreshTokenStore;
import com.ganzithon.backend.auth.domain.StoredRefreshToken;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class RefreshTokenPersistenceAdapter implements RefreshTokenStore {
    private final RefreshTokenJpaRepository repository;

    RefreshTokenPersistenceAdapter(RefreshTokenJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public StoredRefreshToken save(long userId, String tokenHash, Instant expiresAt) {
        return toDomain(repository.save(new RefreshTokenEntity(userId, tokenHash, expiresAt)));
    }

    @Override
    public Optional<StoredRefreshToken> findByHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash).map(RefreshTokenPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<StoredRefreshToken> findByHashForUpdate(String tokenHash) {
        return repository.findByTokenHashForUpdate(tokenHash).map(RefreshTokenPersistenceAdapter::toDomain);
    }

    @Override
    public void revoke(long tokenId) {
        repository.findById(tokenId).ifPresent(token -> token.revoke());
    }

    private static StoredRefreshToken toDomain(RefreshTokenEntity entity) {
        return new StoredRefreshToken(entity.id(), entity.userId(), entity.tokenHash(), entity.expiresAt(), entity.revoked());
    }
}
