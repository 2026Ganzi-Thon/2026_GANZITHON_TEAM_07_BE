package com.ganzithon.backend.counseling.application.port.out;

import com.ganzithon.backend.counseling.domain.CounselingSession;
import com.ganzithon.backend.counseling.domain.CounselingStep;
import com.ganzithon.backend.counseling.domain.MessageSender;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CounselingSessionStore {
    CounselingSession create(long userId, Long malangiSessionId, LocalDateTime now);

    Optional<CounselingSession> find(long userId, long sessionId);

    void save(CounselingSession session);

    boolean delete(long userId, long sessionId);

    long appendMessage(long sessionId, CounselingStep step, MessageSender sender, String content, boolean riskDetected,
            LocalDateTime now);

    void markMessageRisk(long messageId);

    /** 최근 메시지를 오래된 순으로. */
    List<StoredMessage> recentMessages(long sessionId, int limit);

    long saveResult(ResultRecord result);

    boolean ownsMalangiSession(long userId, long malangiSessionId);

    record StoredMessage(long id, MessageSender sender, String content) {}

    record ResultRecord(long sessionId, long userId, String situation, String primaryEmotion, Integer preIntensity,
            int postIntensity, String summary, String recommendedMalangi, List<Map<String, Object>> timeline,
            LocalDateTime createdAt) {}
}
