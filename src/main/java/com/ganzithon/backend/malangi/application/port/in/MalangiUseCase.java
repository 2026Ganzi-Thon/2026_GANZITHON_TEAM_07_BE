package com.ganzithon.backend.malangi.application.port.in;

import com.ganzithon.backend.malangi.domain.MalangiType;
import com.ganzithon.backend.malangi.domain.OwnedMalangi;
import java.time.LocalDateTime;
import java.util.List;

public interface MalangiUseCase {
    List<OwnedMalangi> inventory(long userId);

    Selection select(long userId, String malangiType);

    record Selection(MalangiType selectedMalangiType, LocalDateTime updatedAt) {}
}
