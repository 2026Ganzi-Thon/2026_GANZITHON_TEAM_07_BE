package com.ganzithon.backend.malangi.application.service;

import com.ganzithon.backend.malangi.application.port.in.MalangiFailure;
import com.ganzithon.backend.malangi.application.port.in.MalangiUseCase;
import com.ganzithon.backend.malangi.application.port.out.MalangiInventoryStore;
import com.ganzithon.backend.malangi.domain.MalangiType;
import com.ganzithon.backend.malangi.domain.OwnedMalangi;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class MalangiService implements MalangiUseCase {
    private final MalangiInventoryStore inventory;
    private final Clock clock;

    public MalangiService(MalangiInventoryStore inventory, Clock clock) {
        this.inventory = inventory;
        this.clock = clock;
    }

    @Override
    public List<OwnedMalangi> inventory(long userId) {
        Map<MalangiType, OwnedMalangi> owned = new EnumMap<>(MalangiType.class);
        inventory.findByUserId(userId).forEach(item -> owned.put(item.type(), item));
        return Arrays.stream(MalangiType.values())
                .map(type -> owned.getOrDefault(type, new OwnedMalangi(type, false, false, 0, null)))
                .toList();
    }

    @Override
    public Selection select(long userId, String malangiType) {
        MalangiType type;
        try {
            type = MalangiType.valueOf(malangiType.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new MalangiFailure(MalangiFailure.Code.INVALID_TYPE);
        }
        if (!inventory.selectIfUnlocked(userId, type)) {
            throw new MalangiFailure(MalangiFailure.Code.LOCKED);
        }
        return new Selection(type, LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
    }
}
