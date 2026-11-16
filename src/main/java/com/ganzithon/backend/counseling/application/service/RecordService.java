package com.ganzithon.backend.counseling.application.service;

import com.ganzithon.backend.counseling.application.port.in.CounselingFailure;
import com.ganzithon.backend.counseling.application.port.in.RecordUseCase;
import com.ganzithon.backend.counseling.application.port.out.CounselingSessionStore;
import com.ganzithon.backend.counseling.application.port.out.RecordStore;
import com.ganzithon.backend.counseling.application.port.out.RecordStore.StoredResult;
import com.ganzithon.backend.counseling.domain.CounselingStep;
import java.util.List;
import java.util.Map;

/** F-08 마음 기록함. 정리(STEP_06) 때 timeline 마지막 항목에 남긴 문장들을 함께 돌려준다. */
public class RecordService implements RecordUseCase {
    private static final int MAX_PAGE_SIZE = 50;

    private final RecordStore records;
    private final CounselingSessionStore sessions;

    public RecordService(RecordStore records, CounselingSessionStore sessions) {
        this.records = records;
        this.sessions = sessions;
    }

    @Override
    public RecordPage list(long userId, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) throw new CounselingFailure(CounselingFailure.Code.INVALID_INPUT);
        List<RecordSummary> content = records.findPage(userId, page, size).stream().map(RecordService::summary).toList();
        long total = records.count(userId);
        int totalPages = (int) ((total + size - 1) / size);
        return new RecordPage(content, page, size, total, totalPages, page >= totalPages - 1);
    }

    @Override
    public RecordDetail detail(long userId, long resultId) {
        StoredResult r = find(userId, resultId);
        Map<String, Object> closing = closing(r);
        Map<String, Object> record = closing.get("record") instanceof Map<?, ?> m ? cast(m) : Map.of();
        return new RecordDetail(r.id(), r.sessionId(), r.createdAt(), blankToNull(r.situation()),
                text(closing.get("triggerFactor")), r.primaryEmotion(), text(closing.get("secondaryEmotion")),
                r.preIntensity(), r.postIntensity(), change(r), r.summary(), text(closing.get("keySentence")),
                text(record.get("initialThought")), text(record.get("alternativeThought")),
                text(closing.get("underlyingNeed")), text(closing.get("copingStrategy")), text(record.get("actionPlan")),
                r.recommendedMalangi(), text(closing.get("malangiReason")), r.timeline());
    }

    @Override
    public void delete(long userId, long resultId) {
        StoredResult r = find(userId, resultId);
        if (!sessions.delete(userId, r.sessionId())) throw new CounselingFailure(CounselingFailure.Code.RECORD_NOT_FOUND);
    }

    private StoredResult find(long userId, long resultId) {
        return records.findResult(userId, resultId)
                .orElseThrow(() -> new CounselingFailure(CounselingFailure.Code.RECORD_NOT_FOUND));
    }

    private static RecordSummary summary(StoredResult r) {
        return new RecordSummary(r.id(), r.sessionId(), r.createdAt(), r.primaryEmotion(),
                text(closing(r).get("secondaryEmotion")), r.preIntensity(), r.postIntensity(), change(r), r.summary(),
                r.recommendedMalangi());
    }

    /** 정리 단계(STEP_06) timeline 항목. 없으면 빈 맵. */
    private static Map<String, Object> closing(StoredResult r) {
        if (r.timeline() == null) return Map.of();
        for (int i = r.timeline().size() - 1; i >= 0; i--) {
            Map<String, Object> entry = r.timeline().get(i);
            if (CounselingStep.STEP_06.name().equals(entry.get("step"))) return entry;
        }
        return Map.of();
    }

    private static Integer change(StoredResult r) {
        return r.preIntensity() == null || r.postIntensity() == null ? null : r.postIntensity() - r.preIntensity();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> cast(Map<?, ?> map) {
        return (Map<String, Object>) map;
    }

    private static String text(Object value) {
        return value == null ? null : blankToNull(String.valueOf(value));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
