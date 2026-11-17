package com.ganzithon.backend.counseling.application.port.out;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** counseling_results 조회·숨기기, 월간 분석 레포트 캐시(counseling_reports). */
public interface RecordStore {
    /** 최근 기록부터. from·to(UTC, to는 미포함)가 null이면 기간 제한 없음. */
    List<StoredResult> findPage(long userId, LocalDateTime from, LocalDateTime to, int page, int size);

    long count(long userId, LocalDateTime from, LocalDateTime to);

    /** 기간 안 기록 전부 (오래된 순). */
    List<StoredResult> findAll(long userId, LocalDateTime from, LocalDateTime to);

    Optional<StoredResult> findResult(long userId, long resultId);

    boolean setHidden(long userId, long resultId, boolean hidden);

    Optional<StoredReport> findReport(long userId, int year, int month);

    StoredReport saveReport(long userId, int year, int month, String signature, Map<String, Object> content,
            LocalDateTime now);

    record StoredResult(long id, long sessionId, LocalDateTime createdAt, String situation, String primaryEmotion,
            Integer preIntensity, Integer postIntensity, String summary, String recommendedMalangi,
            List<Map<String, Object>> timeline, boolean hidden) {}

    record StoredReport(int year, int month, String signature, Map<String, Object> content, LocalDateTime createdAt) {}
}
