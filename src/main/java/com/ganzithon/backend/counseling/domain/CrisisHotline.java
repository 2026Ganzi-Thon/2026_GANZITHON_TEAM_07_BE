package com.ganzithon.backend.counseling.domain;

/**
 * 위기 신호 감지 시 안내하는 긴급 상담 연락처입니다.
 * 기준: docs/emotional-dialogue/safety.md 3장(안내 문구)·4장(연락처)
 */
public enum CrisisHotline {
    SUICIDE_PREVENTION("자살예방상담전화", "109", "24시간 연중무휴"),
    MENTAL_HEALTH_CRISIS("정신건강위기상담전화", "1577-0199", "24시간 연중무휴"),
    YOUTH_COUNSELING("청소년 전화", "1388", "24시간 연중무휴"),
    POLICE("긴급 범죄 신고", "112", "24시간 연중무휴"),
    FIRE_RESCUE("긴급 구조 신고", "119", "24시간 연중무휴");

    /** 위기 감지 시 상담을 멈추고 보여 주는 안내 문구입니다. */
    public static final String GUIDANCE_MESSAGE =
            "혼자 감당하지 않아도 괜찮아요. 지금은 마음의 문제를 해결하기보다 안전하게 도움을 받는 것이 중요해요.";

    private final String displayName;
    private final String phoneNumber;
    private final String operatingHours;

    CrisisHotline(String displayName, String phoneNumber, String operatingHours) {
        this.displayName = displayName;
        this.phoneNumber = phoneNumber;
        this.operatingHours = operatingHours;
    }

    public String displayName() {
        return displayName;
    }

    public String phoneNumber() {
        return phoneNumber;
    }

    public String operatingHours() {
        return operatingHours;
    }
}
