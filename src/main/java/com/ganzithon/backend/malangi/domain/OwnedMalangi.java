package com.ganzithon.backend.malangi.domain;

import java.time.LocalDateTime;

public record OwnedMalangi(
        MalangiType type,
        boolean unlocked,
        boolean selected,
        int totalPlayTime,
        LocalDateTime lastInteractedAt
) {}
