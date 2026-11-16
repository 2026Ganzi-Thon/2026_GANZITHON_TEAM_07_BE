package com.ganzithon.backend.counseling.domain;

import java.util.List;

/**
 * 상담 단계 (docs/emotional-dialogue/process.md, 01번 이슈: 서버가 currentStep을 관리한다).
 * 와이어프레임 흐름: STEP_01 대화 → 감정 기록 카드 → STEP_02~05 → 상담 후 강도 → STEP_06 정리.
 */
public enum CounselingStep {
    STEP_01(List.of("situation"), List.of("situation")),
    STEP_02(List.of("initialThought", "triggerFactor"), List.of("initialThought")),
    STEP_03(List.of("thoughtReason", "alternativeThought", "underlyingNeed"), List.of("alternativeThought", "underlyingNeed")),
    STEP_04(List.of("copingStrategy"), List.of("copingStrategy")),
    STEP_05(List.of("changedView", "desiredSituation", "actionPlan"), List.of("actionPlan")),
    STEP_06(List.of(), List.of());

    private final List<String> targets;
    private final List<String> required;

    CounselingStep(List<String> targets, List<String> required) {
        this.targets = targets;
        this.required = required;
    }

    /** 이 단계에서 물을 수 있는 항목 (단계 프롬프트의 "물을 것", questionTarget 값). */
    public List<String> targets() {
        return targets;
    }

    /** 이 단계를 마치려면 확인돼야 하는 슬롯 (값이 있거나 "" = 답 없음). */
    public List<String> required() {
        return required;
    }

    public CounselingStep next() {
        return this == STEP_06 ? STEP_06 : values()[ordinal() + 1];
    }

    /** 사용자와 질문을 주고받는 단계 (STEP_01~05). */
    public boolean conversational() {
        return this != STEP_06;
    }

    /** 되돌아가기 허용 범위 (process.md: 새 사건이면 STEP_01, 바랐던 것을 다시 이야기하면 STEP_03). */
    public boolean canGoBackTo(CounselingStep target) {
        return switch (this) {
            case STEP_03, STEP_05 -> target == STEP_01;
            case STEP_04 -> target == STEP_01 || target == STEP_03;
            default -> false;
        };
    }
}
