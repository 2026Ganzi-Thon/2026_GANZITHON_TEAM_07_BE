package com.ganzithon.backend.counseling.domain;

/**
 * 1차 서버 필터가 구분하는 위기 신호 범주입니다.
 * 기준: docs/emotional-dialogue/safety.md 2장 "감지 대상 위험 범주"
 */
public enum CrisisCategory {
    /** 자해 및 자살 의도: 자해 언급, 자살 계획·시도·생각 암시 */
    SELF_HARM,
    /** 타인 상해 및 폭력: 타인을 해치려는 의도나 복수 계획 */
    HARM_TO_OTHERS,
    /** 가정폭력 및 학대 피해: 현재 진행 중인 신체적·성적·정신적 폭력 피해 */
    ABUSE
}
