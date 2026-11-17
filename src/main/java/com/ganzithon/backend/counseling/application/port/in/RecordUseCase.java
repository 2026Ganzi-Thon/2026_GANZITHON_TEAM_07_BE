package com.ganzithon.backend.counseling.application.port.in;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * F-08 마음 기록함 (+ 와이어프레임의 월간 캘린더·돌아보기·테스형 분석 레포트).
 * 날짜 묶음(캘린더, 기간 조회)과 화면용 date·time(HH:mm)은 서비스 시간대(기본 Asia/Seoul) 기준이고, createdAt은 UTC다.
 */
public interface RecordUseCase {
    /** 최근 기록부터. from·to(서비스 시간대 날짜, 둘 다 포함)가 있으면 그 기간만 (주간 보기). */
    RecordPage list(long userId, int page, int size, LocalDate from, LocalDate to);

    RecordDetail detail(long userId, long resultId);

    /** 상담 대화 다시 보기: 사용자·테스형 말과 대화 안 감정 기록 카드. */
    List<ConversationItem> conversation(long userId, long resultId);

    /** 월간 캘린더: 날짜별 기록과 그 달 자주 느낀 감정. */
    Calendar calendar(long userId, YearMonth month);

    void setHidden(long userId, long resultId, boolean hidden);

    /** 테스형의 월간 분석 레포트. 기록이 바뀌었을 때만 새로 만든다. */
    MonthlyReport monthlyReport(long userId, YearMonth month);

    /** 기록과 연결된 상담 세션(메시지 포함)까지 영구 삭제한다. */
    void delete(long userId, long resultId);

    record RecordSummary(long resultId, long sessionId, LocalDateTime createdAt, LocalDate date, String time,
            String primaryEmotion, String secondaryEmotion, Integer preIntensity, Integer postIntensity,
            Integer intensityChange, String summary, String recommendedMalangi, boolean hidden) {}

    record RecordPage(List<RecordSummary> content, int pageNumber, int pageSize, long totalElements, int totalPages,
            boolean last) {}

    /**
     * 기록함 상세. initialThought·alternativeThought·actionPlan은 정리(STEP_06)에서 다듬은 사용자 1인칭 문장
     * ("그때의 생각", "새로 본 관점", "고른 실천"), 나머지 슬롯은 대화에서 확인된 값. 확인 못 한 칸은 null.
     */
    record RecordDetail(long resultId, long sessionId, LocalDateTime createdAt, LocalDate date, String time,
            String situation, String triggerFactor, String primaryEmotion, String secondaryEmotion,
            Integer preIntensity, Integer postIntensity, Integer intensityChange, String summary, String keySentence,
            String initialThought, String alternativeThought, String underlyingNeed, String copingStrategy,
            String actionPlan, String recommendedMalangi, String malangiReason, boolean hidden,
            List<Map<String, Object>> timeline) {}

    /** type: USER, ASSISTANT, EMOTION_CARD. 카드면 content 대신 emotionCard. */
    record ConversationItem(long messageId, String type, String step, String content, LocalDateTime createdAt,
            String time, SavedEmotionCard emotionCard) {}

    record SavedEmotionCard(String primaryEmotion, String secondaryEmotion, String triggerFactor, Integer intensity) {}

    record CalendarDay(LocalDate date, int count, List<String> emotions, List<RecordSummary> records) {}

    record EmotionCount(String emotion, int count) {}

    record Calendar(int year, int month, int recordCount, List<CalendarDay> days, List<EmotionCount> emotionStats,
            Double averageIntensityChange) {}

    record MonthlyReport(int year, int month, int recordCount, String summary, List<String> thoughtPatterns,
            List<String> helpfulCoping, String changedView, String suggestion, List<EmotionCount> emotionStats,
            LocalDateTime generatedAt) {}
}
