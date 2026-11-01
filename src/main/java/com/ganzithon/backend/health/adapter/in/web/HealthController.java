package com.ganzithon.backend.health.adapter.in.web;

import com.ganzithon.backend.health.application.port.in.CheckHealthUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health", description = "서버 응답 확인")
public class HealthController {

    private final CheckHealthUseCase checkHealthUseCase;

    public HealthController(CheckHealthUseCase checkHealthUseCase) {
        this.checkHealthUseCase = checkHealthUseCase;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "서버 응답 확인", description = "DB·LLM 연결은 검사하지 않습니다.")
    public HealthResponse health() {
        return new HealthResponse(checkHealthUseCase.check().name());
    }
}
