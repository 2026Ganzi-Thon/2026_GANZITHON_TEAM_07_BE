package com.ganzithon.backend.counseling.application.port.out;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** counseling_results 조회. 삭제는 연결된 세션을 지워 결과·메시지까지 함께 지운다. */
public interface RecordStore {
    /** 최근 기록부터. */
    List<StoredResult> findPage(long userId, int page, int size);

    long count(long userId);

    Optional<StoredResult> findResult(long userId, long resultId);

    record StoredResult(long id, long sessionId, LocalDateTime createdAt, String situation, String primaryEmotion,
            Integer preIntensity, Integer postIntensity, String summary, String recommendedMalangi,
            List<Map<String, Object>> timeline) {}
}
