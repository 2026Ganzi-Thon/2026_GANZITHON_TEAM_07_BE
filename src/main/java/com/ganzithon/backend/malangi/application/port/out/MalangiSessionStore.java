package com.ganzithon.backend.malangi.application.port.out;

import com.ganzithon.backend.malangi.domain.MalangiSession;
import com.ganzithon.backend.malangi.domain.MalangiSession.ContextType;
import com.ganzithon.backend.malangi.domain.MalangiSession.EndReason;
import com.ganzithon.backend.malangi.domain.MalangiType;
import java.util.List;
import java.util.Map;

public interface MalangiSessionStore {
    MalangiSession start(long userId, MalangiType type, ContextType contextType);
    MalangiSession end(long userId, long sessionId, int durationSeconds, int touchCount,
            int longPressCount, int dragCount, EndReason endReason, Map<String, Object> customState);
    List<MalangiSession> recent(long userId, int limit);
    int totalPlayTime(long userId, MalangiType type);
}
