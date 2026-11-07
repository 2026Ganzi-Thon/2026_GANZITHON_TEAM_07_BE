package com.ganzithon.backend.auth.application.port.in;

import com.ganzithon.backend.auth.domain.AuthProvider;

/** An application outcome the inbound adapter can translate into an HTTP response. */
public class AuthFailure extends RuntimeException {
    private final Code code;
    private final AuthProvider existingProvider;
    private final String registeredEmail;

    public AuthFailure(Code code) {
        this(code, null, null);
    }

    public AuthFailure(Code code, AuthProvider existingProvider, String registeredEmail) {
        super(code.defaultMessage);
        this.code = code;
        this.existingProvider = existingProvider;
        this.registeredEmail = registeredEmail;
    }

    public Code code() { return code; }
    public AuthProvider existingProvider() { return existingProvider; }
    public String registeredEmail() { return registeredEmail; }

    public enum Code {
        EMAIL_ALREADY_REGISTERED("이미 해당 이메일로 가입된 계정이 있습니다."),
        INVALID_CREDENTIALS("이메일 또는 비밀번호를 확인해 주세요."),
        INVALID_REFRESH_TOKEN("유효하지 않거나 만료된 갱신 토큰입니다."),
        INVALID_SOCIAL_CREDENTIALS("소셜 인증 정보를 확인할 수 없습니다."),
        SOCIAL_PROVIDER_UNAVAILABLE("소셜 로그인 서비스를 일시적으로 사용할 수 없습니다."),
        USER_NOT_FOUND("사용자를 찾을 수 없습니다."),
        INVALID_REQUEST("인증 요청을 확인해 주세요.");

        private final String defaultMessage;

        Code(String defaultMessage) { this.defaultMessage = defaultMessage; }

        public String defaultMessage() { return defaultMessage; }
    }
}
