package com.ganzithon.backend.malangi.application.port.in;

import com.ganzithon.backend.malangi.domain.MalangiType;
import com.ganzithon.backend.malangi.domain.OwnedMalangi;
import com.ganzithon.backend.malangi.domain.MalangiSession;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface MalangiUseCase {
    List<OwnedMalangi> inventory(long userId);

    Selection select(long userId, String malangiType);

    StateUpdate updateState(long userId, String malangiType, Map<String, Object> customState);

    StartedSession startSession(long userId, String malangiType, String contextType);
    CompletedSession endSession(long userId, long sessionId, int durationSeconds, int touchCount,
            int longPressCount, int dragCount, String endReason, Map<String, Object> customState);
    List<MalangiSession> recentSessions(long userId, int limit);

    record Selection(MalangiType selectedMalangiType, LocalDateTime updatedAt) {}
    record StateUpdate(MalangiType malangiType, Map<String, Object> customState, LocalDateTime updatedAt) {}
    record CompletedSession(MalangiSession session, int totalPlayTime) {}
    record StartedSession(MalangiSession session, Map<String, Object> customState) {}
}
