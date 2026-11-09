package com.ganzithon.backend.malangi.adapter.in.web;

import com.ganzithon.backend.common.adapter.in.web.ApiEnvelope;
import com.ganzithon.backend.malangi.application.port.in.MalangiFailure;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class MalangiExceptionHandler {
    @ExceptionHandler(MalangiFailure.class)
    ResponseEntity<ApiEnvelope<Object>> handleMalangiFailure(MalangiFailure failure) {
        HttpStatus status = switch (failure.code()) {
            case LOCKED, SESSION_COMPLETED -> HttpStatus.CONFLICT;
            case SESSION_NOT_FOUND -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(ApiEnvelope.failure(
                failure.code().name(), failure.code().message(), null));
    }
}
