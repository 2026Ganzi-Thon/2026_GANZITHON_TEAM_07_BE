package com.ganzithon.backend.counseling.application.port.in;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** F-08 마음 기록함: 완료한 상담 결과(counseling_results) 조회·삭제. */
public interface RecordUseCase {
    RecordPage list(long userId, int page, int size);

    RecordDetail detail(long userId, long resultId);

    /** 기록과 연결된 상담 세션(메시지 포함)까지 영구 삭제한다. */
    void delete(long userId, long resultId);

    record RecordSummary(long resultId, long sessionId, LocalDateTime createdAt, String primaryEmotion,
            String secondaryEmotion, Integer preIntensity, Integer postIntensity, Integer intensityChange,
            String summary, String recommendedMalangi) {}

    record RecordPage(List<RecordSummary> content, int pageNumber, int pageSize, long totalElements, int totalPages,
            boolean last) {}

    /**
     * 기록함 상세. initialThought·alternativeThought·actionPlan은 정리(STEP_06)에서 다듬은 사용자 1인칭 문장
     * ("그때의 생각", "새로 본 관점", "고른 실천"), 나머지 슬롯은 대화에서 확인된 값. 확인 못 한 칸은 null.
     */
    record RecordDetail(long resultId, long sessionId, LocalDateTime createdAt, String situation,
            String triggerFactor, String primaryEmotion, String secondaryEmotion, Integer preIntensity,
            Integer postIntensity, Integer intensityChange, String summary, String keySentence, String initialThought,
            String alternativeThought, String underlyingNeed, String copingStrategy, String actionPlan,
            String recommendedMalangi, String malangiReason, List<Map<String, Object>> timeline) {}
}
