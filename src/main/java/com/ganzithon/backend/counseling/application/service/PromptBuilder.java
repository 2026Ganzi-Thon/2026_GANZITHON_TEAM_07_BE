package com.ganzithon.backend.counseling.application.service;

import com.ganzithon.backend.counseling.application.port.out.PromptSource;
import com.ganzithon.backend.counseling.domain.CounselingState;
import com.ganzithon.backend.counseling.domain.CounselingStep;
import com.ganzithon.backend.malangi.domain.MalangiType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * prompts/emotion-conversation 조립 (01번 이슈: 공통 + 단계 전용 프롬프트, 04번 이슈: 누적 슬롯을 시스템 문맥에).
 * 캐시가 잘 맞도록 [공통 본문 + 단계 프롬프트]와 [현재 상태 블록]을 나눠 돌려준다.
 */
final class PromptBuilder {
    static final String STATE_MARKER = "# 현재 상태 (서버가 채움)";
    private static final Pattern COMMENT = Pattern.compile("<!--.*?-->", Pattern.DOTALL);
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{(\\w+)}}");

    private final String commonBody;
    private final String stateTemplate;
    private final Map<CounselingStep, String> steps = new HashMap<>();
    private final String safety;

    PromptBuilder(PromptSource source) {
        String common = strip(source.common());
        int marker = common.indexOf(STATE_MARKER);
        if (marker < 0) throw new IllegalStateException("common.md에 상태 블록이 없습니다: " + STATE_MARKER);
        this.commonBody = common.substring(0, marker).strip();
        this.stateTemplate = common.substring(marker).strip();
        for (CounselingStep step : CounselingStep.values()) steps.put(step, strip(source.step(step)));
        this.safety = strip(source.safety());
    }

    record Built(String cachedSystem, String stateSystem) {}

    Built conversation(CounselingStep step, CounselingState state, MalangiType recommended) {
        Map<String, String> fill = new HashMap<>();
        fill.put("currentStep", step.name());
        fill.put("primaryEmotion", state.primaryEmotion());
        fill.put("secondaryEmotion", state.secondaryEmotion());
        fill.put("preIntensity", state.preIntensity() == null ? null : String.valueOf(state.preIntensity()));
        fill.put("postIntensity", state.postIntensity() == null ? null : String.valueOf(state.postIntensity()));
        fill.put("slots", slotsText(state));
        fill.put("answeredTargets", state.answered().isEmpty() ? null : String.join(", ", state.answered()));
        fill.put("nextTarget", StepTargets.next(step, state));
        List<String> later = StepTargets.later(step, state);
        fill.put("laterTargets", later.isEmpty() ? null : String.join(", ", later));
        fill.put("recommendedMalangi", step == CounselingStep.STEP_06 && recommended != null
                ? ReplyRules.malangiLabel(recommended) : null);

        Matcher matcher = PLACEHOLDER.matcher(stateTemplate);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String value = fill.get(matcher.group(1));
            matcher.appendReplacement(out, Matcher.quoteReplacement(value == null ? "없음" : value));
        }
        matcher.appendTail(out);
        return new Built(commonBody + "\n\n" + steps.get(step), out.toString());
    }

    String safetySystem() {
        return safety;
    }

    private static String slotsText(CounselingState state) {
        StringBuilder text = new StringBuilder();
        for (String key : CounselingState.SLOT_KEYS) {
            String value = state.slot(key);
            if (!text.isEmpty()) text.append('\n');
            text.append("  - ").append(key).append(": ").append(value == null ? "아직" : quote(value));
        }
        return text.toString();
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    private static String strip(String text) {
        return COMMENT.matcher(text == null ? "" : text).replaceAll("").strip();
    }
}
