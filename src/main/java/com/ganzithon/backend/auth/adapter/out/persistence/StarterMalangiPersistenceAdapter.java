package com.ganzithon.backend.auth.adapter.out.persistence;

import com.ganzithon.backend.auth.application.port.out.StarterMalangiStore;
import org.springframework.stereotype.Repository;

@Repository
class StarterMalangiPersistenceAdapter implements StarterMalangiStore {
    private final UserMalangiJpaRepository repository;

    StarterMalangiPersistenceAdapter(UserMalangiJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void grantSlangi(long userId) {
        repository.save(new UserMalangiEntity(userId, "SLANGI", true, true));
    }
}
