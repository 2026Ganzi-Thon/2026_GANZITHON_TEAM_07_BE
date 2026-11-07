package com.ganzithon.backend.auth.adapter.out.persistence;

import com.ganzithon.backend.auth.domain.AuthProvider;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface UserJpaRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByProviderAndProviderId(AuthProvider provider, String providerId);
}
