package com.ganzithon.backend.auth.adapter.out.persistence;

import com.ganzithon.backend.auth.application.port.out.AuthUserStore;
import com.ganzithon.backend.auth.domain.AuthProvider;
import com.ganzithon.backend.auth.domain.AuthUser;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class AuthUserPersistenceAdapter implements AuthUserStore {
    private final UserJpaRepository repository;

    AuthUserPersistenceAdapter(UserJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<AuthUser> findByEmail(String email) {
        return repository.findByEmail(email).map(AuthUserPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<AuthUser> findById(long userId) {
        return repository.findById(userId).map(AuthUserPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<AuthUser> findByProviderId(AuthProvider provider, String providerId) {
        return repository.findByProviderAndProviderId(provider, providerId).map(AuthUserPersistenceAdapter::toDomain);
    }

    @Override
    public AuthUser save(AuthUser user) {
        UserEntity entity = new UserEntity(user.email(), user.passwordHash(), user.nickname(), user.provider(), user.providerId());
        return toDomain(repository.save(entity));
    }

    private static AuthUser toDomain(UserEntity entity) {
        return new AuthUser(entity.id(), entity.email(), entity.nickname(), entity.provider(), entity.providerId(), entity.passwordHash());
    }
}
