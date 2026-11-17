package com.ganzithon.backend.counseling.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 진행 중 누적 상태 (counseling_sessions.session_state JSON, docs/database.md 3-3).
 *
 * <p>슬롯 값: null = 아직 확인 전, "" = 사용자가 없다·모르겠다고 답함, 그 외 = 사용자 표현.
 */
public class CounselingState {
    public static final List<String> SLOT_KEYS = List.of("situation", "triggerFactor", "initialThought",
            "alternativeThought", "underlyingNeed", "copingStrategy", "actionPlan");

    private final Map<String, String> slots = new LinkedHashMap<>();
    private String primaryEmotion;
    private String secondaryEmotion;
    private Integer preIntensity;
    private Integer postIntensity;

    // 대화 흐름 (서버 판단용)
    private final List<String> answered = new ArrayList<>();
    private String lastTarget;
    private boolean listened;
    private boolean optionsShown;
    private String backNote;
    private boolean endRequested;
    private NextInput nextInput = NextInput.MESSAGE;
    /** 감정 기록 카드를 막 저장했고 아직 사용자의 다음 말을 받지 않았다 (와이어프레임: 카드 저장 후 사용자 차례) */
    private boolean cardFresh;
    /** 대화 안 감정 기록 카드 (대화 다시 보기용). messageId, primaryEmotion, secondaryEmotion, triggerFactor, intensity */
    private final List<Map<String, Object>> cards = new ArrayList<>();

    // STEP_06 정리 결과
    private String summary;
    private String keySentence;
    private String malangiReason;
    private final Map<String, String> record = new LinkedHashMap<>();

    private final List<Map<String, Object>> timeline = new ArrayList<>();

    public CounselingState() {
        SLOT_KEYS.forEach(key -> slots.put(key, null));
    }

    public String slot(String key) {
        return slots.get(key);
    }

    public boolean resolved(String key) {
        return slots.get(key) != null;
    }

    public void putSlot(String key, String value) {
        if (!SLOT_KEYS.contains(key)) throw new IllegalArgumentException("unknown slot: " + key);
        slots.put(key, value);
    }

    public Map<String, String> slots() {
        return new LinkedHashMap<>(slots);
    }

    public void clearSlots() {
        SLOT_KEYS.forEach(key -> slots.put(key, null));
    }

    public String primaryEmotion() { return primaryEmotion; }
    public String secondaryEmotion() { return secondaryEmotion; }
    public Integer preIntensity() { return preIntensity; }
    public Integer postIntensity() { return postIntensity; }

    public void recordEmotions(String primary, String secondary, Integer preIntensity) {
        this.primaryEmotion = primary;
        this.secondaryEmotion = secondary;
        this.preIntensity = preIntensity;
    }

    public void clearEmotions() {
        recordEmotions(null, null, null);
    }

    public void recordPostIntensity(int postIntensity) {
        this.postIntensity = postIntensity;
    }

    public List<String> answered() { return answered; }
    public String lastTarget() { return lastTarget; }
    public void lastTarget(String target) { this.lastTarget = target; }
    public boolean listened() { return listened; }
    public void listened(boolean listened) { this.listened = listened; }
    public boolean optionsShown() { return optionsShown; }
    public void optionsShown(boolean shown) { this.optionsShown = shown; }
    public String backNote() { return backNote; }
    public void backNote(String note) { this.backNote = note; }
    public boolean endRequested() { return endRequested; }
    public void endRequested(boolean requested) { this.endRequested = requested; }
    public NextInput nextInput() { return nextInput; }
    public boolean cardFresh() { return cardFresh; }
    public void cardFresh(boolean fresh) { this.cardFresh = fresh; }
    public List<Map<String, Object>> cards() { return cards; }

    public void addCard(long messageId) {
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("messageId", messageId);
        card.put("primaryEmotion", primaryEmotion);
        card.put("secondaryEmotion", secondaryEmotion);
        card.put("triggerFactor", slots.get("triggerFactor"));
        card.put("intensity", preIntensity);
        cards.add(card);
    }
    public void nextInput(NextInput input) { this.nextInput = input; }

    public void markAnswered(String target) {
        if (target != null && !answered.contains(target)) answered.add(target);
    }

    /** 새 단계에 들어가면 그 단계의 "물은 것" 기록을 비운다. */
    public void resetStepProgress() {
        answered.clear();
        lastTarget = null;
    }

    public String summary() { return summary; }
    public String keySentence() { return keySentence; }
    public String malangiReason() { return malangiReason; }
    public Map<String, String> record() { return new LinkedHashMap<>(record); }

    public void recordSummary(String summary, String keySentence, String malangiReason, Map<String, String> record) {
        this.summary = summary;
        this.keySentence = keySentence;
        this.malangiReason = malangiReason;
        this.record.clear();
        this.record.putAll(record);
    }

    public List<Map<String, Object>> timeline() {
        return timeline;
    }

    public void addTimeline(Map<String, Object> entry) {
        Map<String, Object> copy = new LinkedHashMap<>();
        copy.put("order", timeline.size() + 1);
        copy.putAll(entry);
        timeline.add(copy);
    }

    // ---------- session_state JSON 변환 ----------

    public Map<String, Object> toMap() {
        Map<String, Object> slotMap = new LinkedHashMap<>(slots);
        slotMap.put("primaryEmotion", primaryEmotion);
        slotMap.put("secondaryEmotion", secondaryEmotion);
        slotMap.put("preIntensity", preIntensity);
        slotMap.put("postIntensity", postIntensity);

        Map<String, Object> flow = new LinkedHashMap<>();
        flow.put("answeredTargets", new ArrayList<>(answered));
        flow.put("lastTarget", lastTarget);
        flow.put("listened", listened);
        flow.put("optionsShown", optionsShown);
        flow.put("backNote", backNote);
        flow.put("endRequested", endRequested);
        flow.put("nextInput", nextInput.name());
        flow.put("cardFresh", cardFresh);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", summary);
        result.put("keySentence", keySentence);
        result.put("malangiReason", malangiReason);
        result.put("record", new LinkedHashMap<>(record));

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("slots", slotMap);
        map.put("flow", flow);
        map.put("result", result);
        map.put("inProgressTimeline", new ArrayList<>(timeline));
        map.put("emotionCards", new ArrayList<>(cards));
        return map;
    }

    @SuppressWarnings("unchecked")
    public static CounselingState fromMap(Map<String, Object> map) {
        CounselingState state = new CounselingState();
        if (map == null) return state;
        Map<String, Object> slotMap = asMap(map.get("slots"));
        SLOT_KEYS.forEach(key -> state.slots.put(key, asString(slotMap.get(key))));
        state.primaryEmotion = asString(slotMap.get("primaryEmotion"));
        state.secondaryEmotion = asString(slotMap.get("secondaryEmotion"));
        state.preIntensity = asInteger(slotMap.get("preIntensity"));
        state.postIntensity = asInteger(slotMap.get("postIntensity"));

        Map<String, Object> flow = asMap(map.get("flow"));
        if (flow.get("answeredTargets") instanceof List<?> list) {
            list.forEach(item -> state.answered.add(String.valueOf(item)));
        }
        state.lastTarget = asString(flow.get("lastTarget"));
        state.listened = Boolean.TRUE.equals(flow.get("listened"));
        state.optionsShown = Boolean.TRUE.equals(flow.get("optionsShown"));
        state.backNote = asString(flow.get("backNote"));
        state.endRequested = Boolean.TRUE.equals(flow.get("endRequested"));
        String next = asString(flow.get("nextInput"));
        state.nextInput = next == null ? NextInput.MESSAGE : NextInput.valueOf(next);
        state.cardFresh = Boolean.TRUE.equals(flow.get("cardFresh"));

        Map<String, Object> result = asMap(map.get("result"));
        state.summary = asString(result.get("summary"));
        state.keySentence = asString(result.get("keySentence"));
        state.malangiReason = asString(result.get("malangiReason"));
        asMap(result.get("record")).forEach((key, value) -> state.record.put(key, asString(value)));

        if (map.get("emotionCards") instanceof List<?> saved) {
            for (Object card : saved) {
                if (card instanceof Map<?, ?> m) state.cards.add(new LinkedHashMap<>((Map<String, Object>) m));
            }
        }
        if (map.get("inProgressTimeline") instanceof List<?> entries) {
            for (Object entry : entries) {
                if (entry instanceof Map<?, ?> m) state.timeline.add(new LinkedHashMap<>((Map<String, Object>) m));
            }
        }
        return state;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Integer asInteger(Object value) {
        if (value instanceof Number number) return number.intValue();
        if (value instanceof String text && !text.isBlank()) return Integer.valueOf(text.strip());
        return null;
    }
}
