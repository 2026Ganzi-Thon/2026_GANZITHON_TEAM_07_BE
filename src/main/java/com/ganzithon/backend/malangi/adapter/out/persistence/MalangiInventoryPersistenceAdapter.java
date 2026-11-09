package com.ganzithon.backend.malangi.adapter.out.persistence;

import com.ganzithon.backend.malangi.application.port.out.MalangiInventoryStore;
import com.ganzithon.backend.malangi.domain.MalangiType;
import com.ganzithon.backend.malangi.domain.OwnedMalangi;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class MalangiInventoryPersistenceAdapter implements MalangiInventoryStore {
    private final UserMalangiJpaRepository repository;

    MalangiInventoryPersistenceAdapter(UserMalangiJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void initializeStarter(long userId) {
        repository.save(new UserMalangiEntity(userId, MalangiType.SLANGI, true, true));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OwnedMalangi> findByUserId(long userId) {
        return repository.findAllByUserIdOrderByMalangiType(userId).stream()
                .map(UserMalangiEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public boolean selectIfUnlocked(long userId, MalangiType type) {
        List<UserMalangiEntity> malangis = repository.findAllByUserIdForUpdate(userId);
        UserMalangiEntity selected = malangis.stream()
                .filter(malangi -> malangi.type() == type)
                .filter(UserMalangiEntity::unlocked)
                .findFirst()
                .orElse(null);
        if (selected == null) {
            return false;
        }
        malangis.forEach(malangi -> malangi.select(malangi == selected));
        return true;
    }

    @Override
    @Transactional
    public boolean updateStateIfOwned(long userId, MalangiType type, Map<String, Object> customState) {
        UserMalangiEntity malangi = repository.findByUserIdAndMalangiType(userId, type.name()).orElse(null);
        if (malangi == null || !malangi.unlocked()) return false;
        malangi.updateCustomState(customState);
        return true;
    }
}
