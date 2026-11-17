package com.ganzithon.backend.counseling.application.service;

import com.ganzithon.backend.counseling.domain.CounselingState;
import com.ganzithon.backend.counseling.domain.CounselingStep;
import java.util.ArrayList;
import java.util.List;

/** 단계별 "이번에 물을 항목"과 "남은 항목" 계산. */
final class StepTargets {
    private StepTargets() {}

    static String next(CounselingStep step, CounselingState state) {
        for (String target : step.targets()) {
            if (done(target, state)) continue;
            if (target.equals("triggerFactor")) continue; // 선택 항목: 사건으로 분명하면 묻지 않는다
            return target;
        }
        return null;
    }

    static List<String> later(CounselingStep step, CounselingState state) {
        String next = next(step, state);
        if (next == null) return List.of();
        List<String> targets = step.targets();
        List<String> later = new ArrayList<>();
        for (String target : targets.subList(targets.indexOf(next) + 1, targets.size())) {
            if (!target.equals("triggerFactor") && !state.answered().contains(target)
                    && !(isSlot(target) && state.resolved(target))) {
                later.add(target);
            }
        }
        return later;
    }

    /** 이미 답을 받았거나 슬롯이 확인된 항목. 상황(situation)은 STEP_01에서 더 물을 수 있다. */
    static boolean done(String target, CounselingState state) {
        if (state.answered().contains(target)) return true;
        return isSlot(target) && state.resolved(target) && !target.equals("situation");
    }

    static boolean isSlot(String key) {
        return CounselingState.SLOT_KEYS.contains(key);
    }

    /** 이번 응답까지 합쳐 단계 필수 항목이 다 확인되는지. actionPlan은 STEP_05에서만 받는다. */
    static boolean wouldFinish(CounselingStep step, CounselingState state, Draft draft) {
        for (String key : step.required()) {
            boolean fromReply = draft.extracted(key) != null
                    && !(key.equals("actionPlan") && step != CounselingStep.STEP_05 && step != CounselingStep.STEP_06);
            if (!state.resolved(key) && !fromReply) return false;
        }
        return true;
    }

    static boolean stepDone(CounselingStep step, CounselingState state) {
        return step.required().stream().allMatch(state::resolved);
    }
}
