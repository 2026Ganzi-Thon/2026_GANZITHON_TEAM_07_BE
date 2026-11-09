package com.ganzithon.backend.malangi.domain;

import java.time.LocalDateTime;
import java.util.Map;

public record OwnedMalangi(
        MalangiType type,
        boolean unlocked,
        boolean selected,
        int totalPlayTime,
        LocalDateTime lastInteractedAt,
        Map<String, Object> customState
) {}
