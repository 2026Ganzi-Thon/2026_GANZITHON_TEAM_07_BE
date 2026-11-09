package com.ganzithon.backend.malangi.application.port.in;

public class MalangiFailure extends RuntimeException {
    private final Code code;

    public MalangiFailure(Code code) {
        super(code.message);
        this.code = code;
    }

    public Code code() {
        return code;
    }

    public enum Code {
        INVALID_TYPE("지원하지 않는 말랑이 종류입니다."),
        INVALID_STATE("커스텀 상태는 JSON 객체여야 합니다."),
        SESSION_NOT_FOUND("말랑이 세션을 찾을 수 없습니다."),
        SESSION_COMPLETED("이미 종료된 말랑이 세션입니다."),
        LOCKED("아직 해금되지 않은 말랑이는 선택할 수 없습니다.");

        private final String message;

        Code(String message) {
            this.message = message;
        }

        public String message() {
            return message;
        }
    }
}
