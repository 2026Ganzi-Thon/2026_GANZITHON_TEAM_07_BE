package com.ganzithon.backend.health.adapter.in.web;

import io.swagger.v3.oas.annotations.media.Schema;

public record HealthResponse(
        @Schema(description = "기동된 서버의 HTTP 응답 상태", example = "UP") String status
) {
}
