package com.ganzithon.backend.malangi.application.service;

import com.ganzithon.backend.malangi.application.port.in.MalangiFailure;
import com.ganzithon.backend.malangi.application.port.in.MalangiUseCase;
import com.ganzithon.backend.malangi.application.port.out.MalangiInventoryStore;
import com.ganzithon.backend.malangi.application.port.out.MalangiSessionStore;
import com.ganzithon.backend.malangi.domain.MalangiSession;
import com.ganzithon.backend.malangi.domain.MalangiSession.ContextType;
import com.ganzithon.backend.malangi.domain.MalangiSession.EndReason;
import com.ganzithon.backend.malangi.domain.MalangiType;
import com.ganzithon.backend.malangi.domain.OwnedMalangi;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class MalangiService implements MalangiUseCase {
    private final MalangiInventoryStore inventory;
    private final Clock clock;
    private final MalangiSessionStore sessions;

    public MalangiService(MalangiInventoryStore inventory, MalangiSessionStore sessions, Clock clock) {
        this.inventory = inventory;
        this.sessions = sessions;
        this.clock = clock;
    }

    @Override
    public List<OwnedMalangi> inventory(long userId) {
        Map<MalangiType, OwnedMalangi> owned = new EnumMap<>(MalangiType.class);
        inventory.findByUserId(userId).forEach(item -> owned.put(item.type(), item));
        return Arrays.stream(MalangiType.values())
                .map(type -> owned.getOrDefault(type, new OwnedMalangi(type, false, false, 0, null,
                        Map.of())))
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

    @Override
    public StateUpdate updateState(long userId, String malangiType, Map<String, Object> customState) {
        MalangiType type = parseType(malangiType);
        if (customState == null) throw new MalangiFailure(MalangiFailure.Code.INVALID_STATE);
        if (!inventory.updateStateIfOwned(userId, type, customState)) throw new MalangiFailure(MalangiFailure.Code.LOCKED);
        return new StateUpdate(type, new LinkedHashMap<>(customState),
                LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
    }

    private MalangiType parseType(String malangiType) {
        try { return MalangiType.valueOf(malangiType.toUpperCase(java.util.Locale.ROOT)); }
        catch (IllegalArgumentException exception) { throw new MalangiFailure(MalangiFailure.Code.INVALID_TYPE); }
    }

    @Override
    public StartedSession startSession(long userId, String malangiType, String contextType) {
        MalangiType type = parseType(malangiType);
        ContextType context;
        try { context = ContextType.valueOf(contextType); }
        catch (IllegalArgumentException exception) { throw new MalangiFailure(MalangiFailure.Code.INVALID_INPUT); }
        MalangiSession session = sessions.start(userId, type, context);
        Map<String, Object> state = inventory.findByUserId(userId).stream().filter(item -> item.type() == type)
                .map(OwnedMalangi::customState).findFirst().orElse(Map.of());
        return new StartedSession(session, state);
    }

    @Override
    public CompletedSession endSession(long userId, long sessionId, int duration, int touch, int press, int drag,
            String reason, Map<String, Object> customState) {
        if (duration < 0 || touch < 0 || press < 0 || drag < 0) throw new MalangiFailure(MalangiFailure.Code.INVALID_INPUT);
        EndReason endReason;
        try { endReason = EndReason.valueOf(reason); }
        catch (IllegalArgumentException exception) { throw new MalangiFailure(MalangiFailure.Code.INVALID_INPUT); }
        MalangiSession session = sessions.end(userId, sessionId, duration, touch, press, drag, endReason, customState);
        return new CompletedSession(session, sessions.totalPlayTime(userId, session.malangiType()));
    }

    @Override
    public List<MalangiSession> recentSessions(long userId, int limit) {
        if (limit < 1 || limit > 100) throw new MalangiFailure(MalangiFailure.Code.INVALID_INPUT);
        return sessions.recent(userId, limit);
    }
}
