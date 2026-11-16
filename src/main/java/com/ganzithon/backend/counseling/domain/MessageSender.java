package com.ganzithon.backend.counseling.domain;

/** counseling_messages.sender_type. SYSTEM은 서버가 대신 보낸 입력(감정 기록 카드, 정리 요청). */
public enum MessageSender {
    USER,
    ASSISTANT,
    SYSTEM
}
