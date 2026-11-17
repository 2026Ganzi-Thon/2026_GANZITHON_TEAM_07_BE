package com.ganzithon.backend.counseling.application.port.in;

public class CounselingFailure extends RuntimeException {
    private final Code code;

    public CounselingFailure(Code code) {
        super(code.message);
        this.code = code;
    }

    public Code code() {
        return code;
    }

    public enum Code {
        INVALID_INPUT("요청 값이 올바르지 않습니다."),
        EMOTION_REQUIRED("대표 감정과 상담 전 감정 강도가 필요합니다."),
        SESSION_NOT_FOUND("상담 세션을 찾을 수 없습니다."),
        RECORD_NOT_FOUND("상담 기록을 찾을 수 없습니다."),
        REPORT_UNAVAILABLE("지금은 분석 레포트를 만들 수 없습니다. 잠시 뒤 다시 시도해 주세요."),
        SESSION_CLOSED("이미 끝난 상담 세션입니다."),
        INPUT_NOT_EXPECTED("지금은 이 입력을 받을 수 없습니다. 응답의 nextInput을 확인해 주세요.");

        private final String message;

        Code(String message) {
            this.message = message;
        }

        public String message() {
            return message;
        }
    }
}
