package com.ganzithon.backend.auth.adapter.in.web;

import com.ganzithon.backend.auth.application.port.in.AuthFailure;
import com.ganzithon.backend.common.adapter.in.web.ApiEnvelope;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(AuthFailure.class)
    ResponseEntity<ApiEnvelope<Object>> handleAuthFailure(AuthFailure failure) {
        HttpStatus status = switch (failure.code()) {
            case EMAIL_ALREADY_REGISTERED -> HttpStatus.CONFLICT;
            case INVALID_CREDENTIALS, INVALID_REFRESH_TOKEN, INVALID_SOCIAL_CREDENTIALS -> HttpStatus.UNAUTHORIZED;
            case SOCIAL_PROVIDER_UNAVAILABLE -> HttpStatus.BAD_GATEWAY;
            case USER_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVALID_REQUEST -> HttpStatus.BAD_REQUEST;
        };
        Object data = failure.code() == AuthFailure.Code.EMAIL_ALREADY_REGISTERED
                ? Map.of("existingProvider", failure.existingProvider().name(), "registeredEmail", failure.registeredEmail())
                : null;
        return ResponseEntity.status(status).body(ApiEnvelope.failure(
                failure.code().name(), failure.getMessage(), data));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiEnvelope<Object>> handleInvalidRequest(MethodArgumentNotValidException exception) {
        return ResponseEntity.badRequest().body(ApiEnvelope.failure(
                "INVALID_INPUT", "요청 값을 확인해 주세요.", null));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiEnvelope<Object>> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(ApiEnvelope.failure("INVALID_INPUT", exception.getMessage(), null));
    }
}
