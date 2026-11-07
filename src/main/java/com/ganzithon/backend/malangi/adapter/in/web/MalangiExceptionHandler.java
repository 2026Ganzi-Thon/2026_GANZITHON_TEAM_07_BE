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
        HttpStatus status = failure.code() == MalangiFailure.Code.LOCKED
                ? HttpStatus.CONFLICT
                : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(ApiEnvelope.failure(
                failure.code().name(), failure.code().message(), null));
    }
}
