package com.ganzithon.backend.counseling.adapter.out.persistence;

import com.ganzithon.backend.counseling.application.port.out.CounselingSessionStore;
import com.ganzithon.backend.counseling.application.port.out.MalangiReward;
import com.ganzithon.backend.counseling.application.port.out.RecordStore;
import com.ganzithon.backend.counseling.domain.CounselingSession;
import com.ganzithon.backend.counseling.domain.CounselingStep;
import com.ganzithon.backend.counseling.domain.MessageSender;
import com.ganzithon.backend.malangi.domain.MalangiType;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class CounselingPersistenceAdapter implements CounselingSessionStore, MalangiReward, RecordStore {
    private final CounselingSessionJpaRepository sessions;
    private final CounselingMessageJpaRepository messages;
    private final CounselingResultJpaRepository results;
    private final CounselingReportJpaRepository reports;
    private final JdbcTemplate jdbc;

    CounselingPersistenceAdapter(CounselingSessionJpaRepository sessions, CounselingMessageJpaRepository messages,
            CounselingResultJpaRepository results, CounselingReportJpaRepository reports, JdbcTemplate jdbc) {
        this.sessions = sessions;
        this.messages = messages;
        this.results = results;
        this.reports = reports;
        this.jdbc = jdbc;
    }

    private static final LocalDateTime MIN_TIME = LocalDateTime.of(1970, 1, 1, 0, 0);
    private static final LocalDateTime MAX_TIME = LocalDateTime.of(9999, 1, 1, 0, 0);

    @Override
    @Transactional
    public CounselingSession create(long userId, Long malangiSessionId, LocalDateTime now) {
        return sessions.save(new CounselingSessionEntity(userId, malangiSessionId, now)).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CounselingSession> find(long userId, long sessionId) {
        return sessions.findOwned(sessionId, userId).map(CounselingSessionEntity::toDomain);
    }

    @Override
    @Transactional
    public void save(CounselingSession session) {
        CounselingSessionEntity entity = sessions.findById(session.id())
                .orElseThrow(() -> new IllegalStateException("counseling session not found: " + session.id()));
        entity.update(session);
    }

    @Override
    @Transactional
    public boolean delete(long userId, long sessionId) {
        if (sessions.findOwned(sessionId, userId).isEmpty()) return false;
        results.deleteBySession(sessionId);
        messages.deleteBySession(sessionId);
        return sessions.deleteOwned(sessionId, userId) > 0;
    }

    @Override
    @Transactional
    public long appendMessage(long sessionId, CounselingStep step, MessageSender sender, String content,
            boolean riskDetected, LocalDateTime now) {
        return messages.save(new CounselingMessageEntity(sessionId, step.name(), sender.name(), content, riskDetected,
                now)).id();
    }

    @Override
    @Transactional
    public void markMessageRisk(long messageId) {
        messages.findById(messageId).ifPresent(CounselingMessageEntity::markRisk);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoredMessage> recentMessages(long sessionId, int limit) {
        List<StoredMessage> recent = new ArrayList<>();
        for (CounselingMessageEntity message : messages.findRecent(sessionId, PageRequest.of(0, limit))) {
            recent.add(stored(message));
        }
        Collections.reverse(recent);
        return recent;
    }

    @Override
    @Transactional
    public long saveResult(ResultRecord r) {
        return results.save(new CounselingResultEntity(r.sessionId(), r.userId(), r.situation(), r.primaryEmotion(),
                r.preIntensity(), r.postIntensity(), r.summary(), r.recommendedMalangi(), r.timeline(),
                r.createdAt())).id();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean ownsMalangiSession(long userId, long malangiSessionId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM malangi_sessions WHERE id = ? AND user_id = ?",
                Integer.class, malangiSessionId, userId);
        return count != null && count > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoredMessage> allMessages(long sessionId) {
        return messages.findAllBySession(sessionId).stream().map(CounselingPersistenceAdapter::stored).toList();
    }

    private static StoredMessage stored(CounselingMessageEntity message) {
        return new StoredMessage(message.id(), MessageSender.valueOf(message.senderType()), message.messageContent(),
                CounselingStep.valueOf(message.step()), message.createdAt());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoredResult> findPage(long userId, LocalDateTime from, LocalDateTime to, int page, int size) {
        return results.findPage(userId, orMin(from), orMax(to), PageRequest.of(page, size)).stream()
                .map(CounselingPersistenceAdapter::stored).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long count(long userId, LocalDateTime from, LocalDateTime to) {
        return results.countByUser(userId, orMin(from), orMax(to));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoredResult> findAll(long userId, LocalDateTime from, LocalDateTime to) {
        return results.findAllBetween(userId, orMin(from), orMax(to)).stream().map(CounselingPersistenceAdapter::stored)
                .toList();
    }

    @Override
    @Transactional
    public boolean setHidden(long userId, long resultId, boolean hidden) {
        CounselingResultEntity result = results.findOwned(resultId, userId).orElse(null);
        if (result == null) return false;
        result.hide(hidden);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoredReport> findReport(long userId, int year, int month) {
        return reports.findPeriod(userId, year, month).map(r -> new StoredReport(r.periodYear(), r.periodMonth(),
                r.sourceSignature(), r.content(), r.createdAt()));
    }

    @Override
    @Transactional
    public StoredReport saveReport(long userId, int year, int month, String signature, Map<String, Object> content,
            LocalDateTime now) {
        CounselingReportEntity report = reports.findPeriod(userId, year, month)
                .orElseGet(() -> new CounselingReportEntity(userId, year, month));
        report.update(signature, content, now);
        reports.save(report);
        return new StoredReport(year, month, signature, content, now);
    }

    private static LocalDateTime orMin(LocalDateTime time) {
        return time == null ? MIN_TIME : time;
    }

    private static LocalDateTime orMax(LocalDateTime time) {
        return time == null ? MAX_TIME : time;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoredResult> findResult(long userId, long resultId) {
        return results.findOwned(resultId, userId).map(CounselingPersistenceAdapter::stored);
    }

    private static StoredResult stored(CounselingResultEntity r) {
        return new StoredResult(r.id(), r.sessionId(), r.createdAt(), r.situation(), r.primaryEmotion(),
                r.preIntensity(), r.postIntensity(), r.summary(), r.recommendedMalangi(),
                r.timeline() == null ? List.of() : new ArrayList<>(r.timeline()), r.hidden());
    }

    /** 추천 말랑이 자동 해금 (features.md F-04). 행이 없으면 해금된 상태로 만든다. */
    @Override
    @Transactional
    public boolean unlock(long userId, MalangiType type) {
        int updated = jdbc.update("UPDATE user_malangis SET is_unlocked = TRUE WHERE user_id = ? AND malangi_type = ? "
                + "AND is_unlocked = FALSE", userId, type.name());
        if (updated > 0) return true;
        Integer owned = jdbc.queryForObject("SELECT COUNT(*) FROM user_malangis WHERE user_id = ? AND malangi_type = ?",
                Integer.class, userId, type.name());
        if (owned != null && owned > 0) return false;
        jdbc.update("INSERT INTO user_malangis (user_id, malangi_type, is_unlocked, is_selected) VALUES (?, ?, TRUE, FALSE)",
                userId, type.name());
        return true;
    }
}
