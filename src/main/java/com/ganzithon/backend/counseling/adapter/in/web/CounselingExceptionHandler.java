package com.ganzithon.backend.counseling.adapter.in.web;

import com.ganzithon.backend.common.adapter.in.web.ApiEnvelope;
import com.ganzithon.backend.counseling.application.port.in.CounselingFailure;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CounselingExceptionHandler {
    @ExceptionHandler(CounselingFailure.class)
    ResponseEntity<ApiEnvelope<Object>> handleCounselingFailure(CounselingFailure failure) {
        HttpStatus status = switch (failure.code()) {
            case SESSION_NOT_FOUND, RECORD_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case SESSION_CLOSED, INPUT_NOT_EXPECTED -> HttpStatus.CONFLICT;
            case INVALID_INPUT, EMOTION_REQUIRED -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(ApiEnvelope.failure(
                failure.code().name(), failure.code().message(), null));
    }
}
