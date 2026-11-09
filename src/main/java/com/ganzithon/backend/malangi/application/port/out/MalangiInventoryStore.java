package com.ganzithon.backend.malangi.application.port.out;

import com.ganzithon.backend.malangi.domain.MalangiType;
import com.ganzithon.backend.malangi.domain.OwnedMalangi;
import java.util.List;
import java.util.Map;

public interface MalangiInventoryStore {
    void initializeStarter(long userId);

    List<OwnedMalangi> findByUserId(long userId);

    boolean selectIfUnlocked(long userId, MalangiType type);

    boolean updateStateIfOwned(long userId, MalangiType type, Map<String, Object> customState);
}
