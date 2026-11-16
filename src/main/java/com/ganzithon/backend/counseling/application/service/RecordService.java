package com.ganzithon.backend.counseling.application.service;

import com.ganzithon.backend.counseling.application.port.in.CounselingFailure;
import com.ganzithon.backend.counseling.application.port.in.RecordUseCase;
import com.ganzithon.backend.counseling.application.port.out.CounselingAi;
import com.ganzithon.backend.counseling.application.port.out.CounselingAi.ReportReply;
import com.ganzithon.backend.counseling.application.port.out.CounselingSessionStore;
import com.ganzithon.backend.counseling.application.port.out.CounselingSessionStore.StoredMessage;
import com.ganzithon.backend.counseling.application.port.out.PromptSource;
import com.ganzithon.backend.counseling.application.port.out.RecordStore;
import com.ganzithon.backend.counseling.application.port.out.RecordStore.StoredReport;
import com.ganzithon.backend.counseling.application.port.out.RecordStore.StoredResult;
import com.ganzithon.backend.counseling.domain.CounselingSession;
import com.ganzithon.backend.counseling.domain.CounselingStep;
import com.ganzithon.backend.counseling.domain.MessageSender;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * F-08 마음 기록함과 와이어프레임의 월간 캘린더·돌아보기·테스형 분석 레포트.
 * 정리(STEP_06) 때 timeline 마지막 항목에 남긴 문장들을 함께 돌려준다.
 */
public class RecordService implements RecordUseCase {
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_REPORT_RECORDS = 30;
    private static final Pattern CARD_TEXT = Pattern.compile("^\\(감정 기록: ([^,/]+)(?:, ([^/]+))? / 강도 (\\d+)\\)$");
    private static final Pattern COMMENT = Pattern.compile("<!--.*?-->", Pattern.DOTALL);

    private final RecordStore records;
    private final CounselingSessionStore sessions;
    private final CounselingAi ai;
    private final PromptSource prompts;
    private final Clock clock;
    private final ZoneId zone;

    public RecordService(RecordStore records, CounselingSessionStore sessions, CounselingAi ai, PromptSource prompts,
            Clock clock, ZoneId zone) {
        this.records = records;
        this.sessions = sessions;
        this.ai = ai;
        this.prompts = prompts;
        this.clock = clock;
        this.zone = zone;
    }

    @Override
    public RecordPage list(long userId, int page, int size, LocalDate from, LocalDate to) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE || (from != null && to != null && to.isBefore(from))) {
            throw new CounselingFailure(CounselingFailure.Code.INVALID_INPUT);
        }
        LocalDateTime start = from == null ? null : utc(from.atStartOfDay(zone));
        LocalDateTime end = to == null ? null : utc(to.plusDays(1).atStartOfDay(zone));
        List<RecordSummary> content = records.findPage(userId, start, end, page, size).stream().map(this::summary)
                .toList();
        long total = records.count(userId, start, end);
        int totalPages = (int) ((total + size - 1) / size);
        return new RecordPage(content, page, size, total, totalPages, page >= totalPages - 1);
    }

    @Override
    public RecordDetail detail(long userId, long resultId) {
        StoredResult r = find(userId, resultId);
        Map<String, Object> closing = closing(r);
        Map<String, Object> record = polished(closing);
        ZonedDateTime local = local(r.createdAt());
        return new RecordDetail(r.id(), r.sessionId(), r.createdAt(), local.toLocalDate(), minute(local),
                blankToNull(r.situation()), text(closing.get("triggerFactor")), r.primaryEmotion(),
                text(closing.get("secondaryEmotion")), r.preIntensity(), r.postIntensity(), change(r), r.summary(),
                text(closing.get("keySentence")), text(record.get("initialThought")),
                text(record.get("alternativeThought")), text(closing.get("underlyingNeed")),
                text(closing.get("copingStrategy")), text(record.get("actionPlan")), r.recommendedMalangi(),
                text(closing.get("malangiReason")), r.hidden(), r.timeline());
    }

    @Override
    public List<ConversationItem> conversation(long userId, long resultId) {
        StoredResult r = find(userId, resultId);
        Map<Long, Map<String, Object>> cards = new LinkedHashMap<>();
        sessions.find(userId, r.sessionId()).map(CounselingSession::state).ifPresent(state -> state.cards()
                .forEach(card -> cards.put(((Number) card.get("messageId")).longValue(), card)));
        List<ConversationItem> items = new ArrayList<>();
        for (StoredMessage m : sessions.allMessages(r.sessionId())) {
            String step = m.step() == null ? null : m.step().name();
            String time = m.createdAt() == null ? null : minute(local(m.createdAt()));
            if (m.sender() == MessageSender.SYSTEM) {
                SavedEmotionCard card = card(cards.get(m.id()), m.content());
                if (card != null) items.add(new ConversationItem(m.id(), "EMOTION_CARD", step, null, m.createdAt(), time, card));
                continue; // 정리 요청 같은 서버 입력은 보여 주지 않는다
            }
            items.add(new ConversationItem(m.id(), m.sender().name(), step, m.content(), m.createdAt(), time, null));
        }
        return items;
    }

    @Override
    public Calendar calendar(long userId, YearMonth requested) {
        YearMonth month = month(requested);
        List<StoredResult> all = monthRecords(userId, month);
        Map<LocalDate, List<RecordSummary>> byDay = new TreeMap<>();
        for (StoredResult r : all) {
            RecordSummary s = summary(r);
            byDay.computeIfAbsent(s.date(), d -> new ArrayList<>()).add(s);
        }
        List<CalendarDay> days = new ArrayList<>();
        byDay.forEach((date, list) -> {
            LinkedHashSet<String> emotions = new LinkedHashSet<>();
            list.forEach(s -> { if (s.primaryEmotion() != null) emotions.add(s.primaryEmotion()); });
            days.add(new CalendarDay(date, list.size(), List.copyOf(emotions), list));
        });
        return new Calendar(month.getYear(), month.getMonthValue(), all.size(), days, emotionStats(all),
                averageChange(all));
    }

    @Override
    public void setHidden(long userId, long resultId, boolean hidden) {
        if (!records.setHidden(userId, resultId, hidden)) {
            throw new CounselingFailure(CounselingFailure.Code.RECORD_NOT_FOUND);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public MonthlyReport monthlyReport(long userId, YearMonth requested) {
        YearMonth month = month(requested);
        List<StoredResult> all = monthRecords(userId, month);
        List<EmotionCount> stats = emotionStats(all);
        if (all.isEmpty()) {
            return new MonthlyReport(month.getYear(), month.getMonthValue(), 0, null, List.of(), List.of(), null, null,
                    stats, null);
        }
        String signature = all.size() + ":" + all.stream().mapToLong(StoredResult::id).max().orElse(0);
        StoredReport cached = records.findReport(userId, month.getYear(), month.getMonthValue())
                .filter(report -> report.signature().equals(signature)).orElse(null);
        if (cached == null) {
            ReportReply reply = generate(month, all, stats);
            Map<String, Object> content = new LinkedHashMap<>();
            content.put("summary", reply.summary());
            content.put("thoughtPatterns", reply.thoughtPatterns());
            content.put("helpfulCoping", reply.helpfulCoping());
            content.put("changedView", reply.changedView());
            content.put("suggestion", reply.suggestion());
            cached = records.saveReport(userId, month.getYear(), month.getMonthValue(), signature, content,
                    LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS));
        }
        Map<String, Object> c = cached.content();
        return new MonthlyReport(month.getYear(), month.getMonthValue(), all.size(), text(c.get("summary")),
                strings(c.get("thoughtPatterns")), strings(c.get("helpfulCoping")), text(c.get("changedView")),
                text(c.get("suggestion")), stats, cached.createdAt());
    }

    @Override
    public void delete(long userId, long resultId) {
        StoredResult r = find(userId, resultId);
        if (!sessions.delete(userId, r.sessionId())) throw new CounselingFailure(CounselingFailure.Code.RECORD_NOT_FOUND);
    }

    // ---------- 레포트 ----------

    private ReportReply generate(YearMonth month, List<StoredResult> all, List<EmotionCount> stats) {
        StringBuilder input = new StringBuilder();
        input.append("[기간] ").append(month.getYear()).append("년 ").append(month.getMonthValue()).append("월\n");
        input.append("[기록 수] ").append(all.size()).append("\n[자주 느낀 감정] ");
        input.append(String.join(", ", stats.stream().map(s -> s.emotion() + " " + s.count() + "회").toList()));
        input.append("\n");
        List<StoredResult> recent = all.size() > MAX_REPORT_RECORDS ? all.subList(all.size() - MAX_REPORT_RECORDS, all.size()) : all;
        int index = 1;
        for (StoredResult r : recent) {
            Map<String, Object> closing = closing(r);
            Map<String, Object> record = polished(closing);
            ZonedDateTime local = local(r.createdAt());
            input.append("\n[기록 ").append(index++).append("] ").append(local.getMonthValue()).append("월 ")
                    .append(local.getDayOfMonth()).append("일 / 대표 감정 ").append(nz(r.primaryEmotion()));
            String secondary = text(closing.get("secondaryEmotion"));
            if (secondary != null) input.append(", 부가 감정 ").append(secondary);
            input.append(" / 강도 ").append(r.preIntensity()).append(" → ").append(r.postIntensity()).append('\n');
            line(input, "상황", r.situation());
            line(input, "그때의 생각", text(record.get("initialThought")));
            line(input, "새로 본 관점", text(record.get("alternativeThought")));
            line(input, "바랐던 것", text(closing.get("underlyingNeed")));
            line(input, "버텨 온 방법", text(closing.get("copingStrategy")));
            line(input, "고른 실천", text(record.get("actionPlan")));
        }
        String system = COMMENT.matcher(prompts.monthlyReport()).replaceAll("").strip();
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                ReportReply reply = ai.monthlyReport(system, input.toString());
                if (reply != null && reply.summary() != null && !reply.summary().isBlank()) return reply;
            } catch (CounselingAi.Unavailable exception) {
                // 한 번 더 시도
            }
        }
        throw new CounselingFailure(CounselingFailure.Code.REPORT_UNAVAILABLE);
    }

    private static void line(StringBuilder out, String label, String value) {
        if (value != null && !value.isBlank()) out.append("- ").append(label).append(": ").append(value).append('\n');
    }

    // ---------- 공통 ----------

    /** 요청한 달, 없으면 서비스 시간대 기준 이번 달. */
    private YearMonth month(YearMonth requested) {
        return requested != null ? requested : YearMonth.from(clock.instant().atZone(zone));
    }

    private List<StoredResult> monthRecords(long userId, YearMonth month) {
        return records.findAll(userId, utc(month.atDay(1).atStartOfDay(zone)),
                utc(month.plusMonths(1).atDay(1).atStartOfDay(zone)));
    }

    private StoredResult find(long userId, long resultId) {
        return records.findResult(userId, resultId)
                .orElseThrow(() -> new CounselingFailure(CounselingFailure.Code.RECORD_NOT_FOUND));
    }

    private RecordSummary summary(StoredResult r) {
        ZonedDateTime local = local(r.createdAt());
        return new RecordSummary(r.id(), r.sessionId(), r.createdAt(), local.toLocalDate(), minute(local),
                r.primaryEmotion(), text(closing(r).get("secondaryEmotion")), r.preIntensity(), r.postIntensity(),
                change(r), r.summary(), r.recommendedMalangi(), r.hidden());
    }

    private static List<EmotionCount> emotionStats(List<StoredResult> all) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        all.forEach(r -> { if (r.primaryEmotion() != null) counts.merge(r.primaryEmotion(), 1, Integer::sum); });
        return counts.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .map(e -> new EmotionCount(e.getKey(), e.getValue()))
                .toList();
    }

    private static Double averageChange(List<StoredResult> all) {
        List<Integer> changes = all.stream().map(RecordService::change).filter(c -> c != null).toList();
        if (changes.isEmpty()) return null;
        double avg = changes.stream().mapToInt(Integer::intValue).average().orElse(0);
        return Math.round(avg * 10) / 10.0;
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

    @SuppressWarnings("unchecked")
    private static Map<String, Object> polished(Map<String, Object> closing) {
        return closing.get("record") instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    private static SavedEmotionCard card(Map<String, Object> saved, String content) {
        if (saved != null) {
            Object intensity = saved.get("intensity");
            return new SavedEmotionCard(text(saved.get("primaryEmotion")), text(saved.get("secondaryEmotion")),
                    text(saved.get("triggerFactor")), intensity instanceof Number n ? n.intValue() : null);
        }
        Matcher m = CARD_TEXT.matcher(content == null ? "" : content.strip());
        if (!m.matches()) return null;
        return new SavedEmotionCard(m.group(1).strip(), m.group(2) == null ? null : m.group(2).strip(), null,
                Integer.valueOf(m.group(3)));
    }

    private static Integer change(StoredResult r) {
        return r.preIntensity() == null || r.postIntensity() == null ? null : r.postIntensity() - r.preIntensity();
    }

    private ZonedDateTime local(LocalDateTime utc) {
        return utc.atZone(ZoneOffset.UTC).withZoneSameInstant(zone);
    }

    private static LocalDateTime utc(ZonedDateTime time) {
        return time.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    private static String minute(ZonedDateTime time) {
        return time.toLocalTime().truncatedTo(ChronoUnit.MINUTES).toString(); // HH:mm
    }

    private static List<String> strings(Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        return list.stream().filter(item -> item != null && !String.valueOf(item).isBlank()).map(String::valueOf).toList();
    }

    private static String text(Object value) {
        return value == null ? null : blankToNull(String.valueOf(value));
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
