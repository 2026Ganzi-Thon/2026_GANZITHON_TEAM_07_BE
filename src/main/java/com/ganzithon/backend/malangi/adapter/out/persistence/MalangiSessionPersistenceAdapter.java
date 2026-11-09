package com.ganzithon.backend.malangi.adapter.out.persistence;

import com.ganzithon.backend.malangi.application.port.in.MalangiFailure;
import com.ganzithon.backend.malangi.application.port.out.MalangiSessionStore;
import com.ganzithon.backend.malangi.domain.MalangiSession;
import com.ganzithon.backend.malangi.domain.MalangiSession.ContextType;
import com.ganzithon.backend.malangi.domain.MalangiSession.EndReason;
import com.ganzithon.backend.malangi.domain.MalangiType;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class MalangiSessionPersistenceAdapter implements MalangiSessionStore {
    private final MalangiSessionJpaRepository sessions;
    private final UserMalangiJpaRepository inventory;
    private final Clock clock;
    MalangiSessionPersistenceAdapter(MalangiSessionJpaRepository sessions, UserMalangiJpaRepository inventory, Clock clock) {
        this.sessions = sessions; this.inventory = inventory; this.clock = clock;
    }
    @Override @Transactional
    public MalangiSession start(long userId, MalangiType type, ContextType contextType) {
        UserMalangiEntity malangi = inventory.findByUserIdAndMalangiType(userId, type.name()).orElse(null);
        if (malangi == null || !malangi.unlocked()) throw new MalangiFailure(MalangiFailure.Code.LOCKED);
        return sessions.saveAndFlush(new MalangiSessionEntity(userId, type, contextType,
                LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC))).toDomain();
    }
    @Override @Transactional
    public MalangiSession end(long userId, long sessionId, int duration, int touch, int press, int drag,
            EndReason reason, Map<String, Object> state) {
        MalangiSessionEntity session = sessions.findByIdForUpdate(sessionId)
                .filter(item -> item.userId() == userId).orElseThrow(() -> new MalangiFailure(MalangiFailure.Code.SESSION_NOT_FOUND));
        if (!session.inProgress()) throw new MalangiFailure(MalangiFailure.Code.SESSION_COMPLETED);
        UserMalangiEntity malangi = inventory.findByUserIdAndMalangiType(userId, session.type().name())
                .orElseThrow(() -> new MalangiFailure(MalangiFailure.Code.LOCKED));
        LocalDateTime endedAt = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        session.complete(duration, touch, press, drag, reason, endedAt);
        if (state != null) malangi.updateCustomState(state);
        malangi.recordPlay(duration, endedAt);
        sessions.flush();
        return session.toDomain();
    }
    @Override @Transactional(readOnly = true)
    public List<MalangiSession> recent(long userId, int limit) {
        return sessions.findAllByUserIdOrderByStartedAtDesc(userId, PageRequest.of(0, limit)).stream()
                .map(MalangiSessionEntity::toDomain).toList();
    }
    @Override @Transactional(readOnly = true)
    public int totalPlayTime(long userId, MalangiType type) {
        return inventory.findByUserIdAndMalangiType(userId, type.name()).map(UserMalangiEntity::totalPlayTime).orElse(0);
    }
}
