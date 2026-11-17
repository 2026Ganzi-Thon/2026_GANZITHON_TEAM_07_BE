package com.ganzithon.backend.counseling.adapter.in.web;

import com.ganzithon.backend.common.adapter.in.web.ApiEnvelope;
import com.ganzithon.backend.counseling.application.port.in.RecordUseCase;
import com.ganzithon.backend.counseling.application.port.in.RecordUseCase.RecordDetail;
import com.ganzithon.backend.counseling.application.port.in.RecordUseCase.RecordPage;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** F-08 마음 기록함 API. */
@RestController
@RequestMapping("/api/v1/records")
public class RecordController {
    private final RecordUseCase records;

    public RecordController(RecordUseCase records) {
        this.records = records;
    }

    @GetMapping
    public ApiEnvelope<RecordPage> list(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiEnvelope.success(records.list(userId(jwt), page, size));
    }

    @GetMapping("/{resultId}")
    public ApiEnvelope<RecordDetail> detail(@AuthenticationPrincipal Jwt jwt, @PathVariable long resultId) {
        return ApiEnvelope.success(records.detail(userId(jwt), resultId));
    }

    @DeleteMapping("/{resultId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable long resultId) {
        records.delete(userId(jwt), resultId);
    }

    private static long userId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
