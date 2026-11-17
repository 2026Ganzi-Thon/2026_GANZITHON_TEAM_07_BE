package com.ganzithon.backend.counseling.domain;

/** 클라이언트가 다음에 보여 줄 입력 (와이어프레임). */
public enum NextInput {
    /** 일반 채팅 입력 */
    MESSAGE,
    /** 대화 안 감정 기록 카드 (대표·부가 감정, 유발 상황, 상담 전 강도) */
    EMOTION_CARD,
    /** 상담 후 감정 강도 입력 → 정리 */
    POST_INTENSITY,
    /** 입력 없음 (위기 안내, 완료, 중단) */
    NONE
}
