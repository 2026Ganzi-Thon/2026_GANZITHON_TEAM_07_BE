package com.ganzithon.backend.counseling.application.service;

import com.ganzithon.backend.counseling.domain.CounselingState;
import com.ganzithon.backend.counseling.domain.CounselingStep;
import com.ganzithon.backend.counseling.domain.EmotionCatalog;
import com.ganzithon.backend.malangi.domain.MalangiType;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * AI 응답 검증과 서버 보정 (05번 이슈: 서버가 검증 후 저장·전환).
 * 규칙에 어긋나면 이유를 돌려주고, 서비스가 그 이유를 붙여 한 번 다시 요청한다.
 * 보정으로 처리할 수 있는 것(질문 줄 지우기, 허용되지 않은 칩 버리기)은 Draft를 고치고 통과시킨다.
 */
final class ReplyRules {
    private static final Pattern HAEYO = Pattern.compile("(요[.?!]?$|요[.?!] |니다[.?!]?)");
    private static final Pattern RECORD_HAEYO = Pattern.compile("요[.?!”\"]?$");
    private static final Pattern SENTENCE_END = Pattern.compile("(?<=[.!])\\s+");

    private ReplyRules() {}

    static String check(Draft d, CounselingStep step, CounselingState state, boolean listen, MalangiType recommended) {
        String msg = d.userMessage;
        if (msg.isBlank()) return "userMessage가 비었다";
        if (count(msg, '?') > 1) return "질문이 두 개 이상이다. 질문은 하나만";
        if (d.endRequested) return null;

        if (step == CounselingStep.STEP_06) {
            if (d.asks()) return "정리에 질문이 있다. 질문하지 않는다";
            List<String> missing = new ArrayList<>();
            for (String key : List.of("initialThought", "alternativeThought")) {
                if (notEmpty(state.slot(key)) && !notEmpty(d.extracted(key))) missing.add(key);
            }
            if (!missing.isEmpty()) {
                return "기록함 문장 " + missing + "이 비었다. 확인된 슬롯 값을 사용자 1인칭 해요체 한 문장으로 다시 채운다";
            }
            List<String> bad = new ArrayList<>();
            for (String key : List.of("initialThought", "alternativeThought", "summary")) {
                String value = d.extracted(key);
                if (notEmpty(value) && !RECORD_HAEYO.matcher(value.strip()).find()) bad.add(key);
            }
            if (!bad.isEmpty()) return "기록함 문장 " + bad + "은 사용자 1인칭 해요체 한 문장으로 끝낸다 (~요.)";
            String reason = d.extracted("malangiReason") == null ? "" : d.extracted("malangiReason");
            for (MalangiType other : MalangiType.values()) {
                if (other != recommended && reason.contains(other.displayName())) {
                    return "malangiReason은 추천 말랑이 " + recommended.displayName() + "만 말한다 (다른 말랑이 이름 금지)";
                }
            }
            return null;
        }

        if (HAEYO.matcher(msg.replace("\n", " ").strip()).find()) {
            return "테스형 하오체가 아닌 해요체·합니다체 어미가 있다. '~소', '~구려', '~겠소?'로 끝낸다";
        }
        if (step == CounselingStep.STEP_01 && !d.transitionReady && msg.contains("정리해 보면")) {
            return "STEP_01을 마치지 않았는데 마무리 문장을 썼다. 상황이 부족하면 상황을 묻는 질문 하나로 끝낸다 (questionTarget: situation)";
        }
        boolean middle = step != CounselingStep.STEP_01;
        if (middle && !listen && d.transitionReady && !d.asks() && d.replyOptions.isEmpty()
                && !StepTargets.wouldFinish(step, state, d)) {
            return step + "의 필수 항목 " + step.required() + "이 아직 확인되지 않았다. 받아 준 뒤 "
                    + StepTargets.next(step, state) + "을 묻는다. transitionReady: false";
        }
        if (d.backToStep != null && step.canGoBackTo(parseStep(d.backToStep))) {
            return null; // 되돌아가면 서버가 돌아간 단계 질문을 다시 받아 오므로 이 응답의 질문은 쓰지 않는다
        }
        if (listen) return d.asks() ? "들어 달라고 했는데 질문했다. 질문 없이 받아 주는 말만" : null;

        if (!d.replyOptions.isEmpty() && (step == CounselingStep.STEP_01 || step == CounselingStep.STEP_03
                || step == CounselingStep.STEP_04)) {
            d.replyOptions = new ArrayList<>(); // 다른 해석·바란 것을 대신 제시하지 않는다 → 서버가 버린다
            d.optionsDropped = true;
        }
        if (d.transitionReady && step == CounselingStep.STEP_05 && d.asks() && msg.contains("\n")
                && StepTargets.wouldFinish(step, state, d)) {
            d.userMessage = msg.split("\n")[0].strip(); // 행동이 정해졌는데 확인 질문을 덧붙임 → 질문 줄을 지운다
            d.serverFinished = true;
            return null;
        }
        if (d.transitionReady && (step == CounselingStep.STEP_01 || step == CounselingStep.STEP_05) && d.asks()) {
            return "단계를 마칠 때는 질문하지 않는다. " + (step == CounselingStep.STEP_01
                    ? "받아 주는 한 문장 + 감정을 먼저 같이 정리해 보자는 한 문장" : "받아 주는 한 문장만");
        }
        if (d.asks() && (step == CounselingStep.STEP_02 || step == CounselingStep.STEP_03
                || step == CounselingStep.STEP_04) && StepTargets.wouldFinish(step, state, d)) {
            // 이번 답으로 필수 항목이 다 찼는데 질문을 덧붙임 → 단계 완료로 처리하고 다음 단계 첫 질문을 다시 받는다
            d.transitionReady = true;
            d.serverFinished = true;
            if (msg.contains("\n")) d.userMessage = msg.split("\n")[0].strip();
            return null;
        }
        String target = d.questionTarget;
        if (d.asks() && target == null && !d.transitionReady) {
            return "질문을 했는데 questionTarget이 없다. " + step + "의 물을 것 " + step.targets() + " 중 하나만 묻는다";
        }
        if (d.asks() && target != null) {
            if (!step.targets().contains(target)) {
                return step + "에서 물을 수 없는 항목(" + target + ")을 물었다. 물을 것: " + step.targets();
            }
            if (StepTargets.done(target, state)) return "이미 확인된 항목(" + target + ")을 다시 물었다. 다시 묻지 않는다";
        }
        return null;
    }

    /** 공감 문장과 질문 문장 사이 줄바꿈이 빠졌으면 마지막 문장 앞에서 나눈다 (말풍선 분리). */
    static String splitBubble(String msg) {
        if (!msg.contains("?") || msg.contains("\n")) return msg;
        String[] parts = SENTENCE_END.split(msg.strip());
        if (parts.length < 2) return msg;
        return String.join(" ", List.of(parts).subList(0, parts.length - 1)) + "\n" + parts[parts.length - 1];
    }

    static CounselingStep parseStep(String value) {
        try {
            return value == null ? null : CounselingStep.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    static boolean notEmpty(String value) {
        return value != null && !value.isBlank();
    }

    static String malangiLabel(MalangiType type) {
        return type.displayName() + " (" + EmotionCatalog.describe(type) + ")";
    }

    private static int count(String text, char c) {
        int n = 0;
        for (int i = 0; i < text.length(); i++) if (text.charAt(i) == c) n++;
        return n;
    }
}
