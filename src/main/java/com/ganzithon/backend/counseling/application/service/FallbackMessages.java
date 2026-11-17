package com.ganzithon.backend.counseling.application.service;

import com.ganzithon.backend.counseling.domain.CounselingStep;

/**
 * LLM 호출 최종 실패 시 보내는 단계별 기본 문장 (07번 이슈). 테스형 하오체.
 * retry: 같은 단계에서 실패, start: 단계가 넘어간 뒤 새 단계 첫 질문을 받아 오다 실패.
 */
final class FallbackMessages {
    static final String SUMMARY = "오늘 이야기는 잘 남겨 두었소.\n정리 내용은 마음 기록함에서 다시 볼 수 있소.";
    static final String SAFETY_CHECK_FAILED =
            "이야기해 줘서 고마워요. 지금 마음이 많이 무거울 수 있어요. 혼자 감당하기 어렵다면 언제든 도움을 받을 수 있어요.";

    private FallbackMessages() {}

    static String retry(CounselingStep step) {
        return switch (step) {
            case STEP_01 -> "잠시 답을 준비하지 못했소.\n어떤 일이 있었는지 한 번만 더 들려주겠소?";
            case STEP_02 -> "잠시 답을 준비하지 못했소.\n그 순간 가장 먼저 떠오른 생각을 한 번만 더 들려주겠소?";
            case STEP_03 -> "잠시 답을 준비하지 못했소.\n방금 한 이야기를 한 번만 더 들려주겠소?";
            case STEP_04 -> "잠시 답을 준비하지 못했소.\n힘들 때 도움이 되었던 방법을 한 번만 더 들려주겠소?";
            case STEP_05 -> "잠시 답을 준비하지 못했소.\n해 보고 싶은 작은 행동을 한 번만 더 들려주겠소?";
            case STEP_06 -> SUMMARY;
        };
    }

    static String start(CounselingStep step) {
        return switch (step) {
            case STEP_02 -> "이야기해 주어 고맙소.\n그 감정이 올라오던 순간 머릿속에 가장 먼저 스친 생각은 무엇이었소?";
            case STEP_03 -> "그런 생각이 들 만큼 마음이 무거웠구려.\n그렇게 생각하게 된 이유는 무엇이었소?";
            case STEP_04 -> "그 마음을 꺼내 주어 고맙소.\n이렇게 힘들 때 지금까지 어떻게 버텨 왔소?";
            case STEP_05 -> "그렇게 버텨 왔구려.\n처음 생각과 비교했을 때, 지금 달라진 부분이 있소?";
            default -> retry(step);
        };
    }
}
