package com.ganzithon.backend.malangi.application.port.out;

import com.ganzithon.backend.malangi.domain.MalangiType;
import com.ganzithon.backend.malangi.domain.OwnedMalangi;
import java.util.List;

public interface MalangiInventoryStore {
    void initializeStarter(long userId);

    List<OwnedMalangi> findByUserId(long userId);

    boolean selectIfUnlocked(long userId, MalangiType type);
}
