package com.ganzithon.backend.malangi.adapter.in.web;

import com.ganzithon.backend.common.adapter.in.web.ApiEnvelope;
import com.ganzithon.backend.malangi.application.port.in.MalangiUseCase;
import com.ganzithon.backend.malangi.domain.MalangiSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/malangi/sessions")
public class MalangiSessionController {
    private final MalangiUseCase malangi;
    public MalangiSessionController(MalangiUseCase malangi) { this.malangi = malangi; }

    @PostMapping("/start")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiEnvelope<SessionData> start(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody StartRequest request) {
        return ApiEnvelope.success(SessionData.from(malangi.startSession(userId(jwt), request.malangiType(), request.contextType())));
    }

    @PostMapping("/{sessionId}/end")
    public ApiEnvelope<EndData> end(@AuthenticationPrincipal Jwt jwt, @PathVariable long sessionId,
            @Valid @RequestBody EndRequest request) {
        var result = malangi.endSession(userId(jwt), sessionId, request.durationSeconds(), request.touchCount(),
                request.longPressCount(), request.dragCount(), request.endReason(), request.customState());
        return ApiEnvelope.success(EndData.from(result));
    }

    @GetMapping
    public ApiEnvelope<SessionsData> recent(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "10") @Min(1) int limit) {
        return ApiEnvelope.success(new SessionsData(malangi.recentSessions(userId(jwt), limit).stream()
                .map(SessionData::from).toList()));
    }

    private static long userId(Jwt jwt) { return Long.parseLong(jwt.getSubject()); }
    public record StartRequest(@NotBlank String malangiType, @NotBlank String contextType) {}
    public record EndRequest(@Min(0) int durationSeconds, @Min(0) int touchCount,
            @Min(0) int longPressCount, @Min(0) int dragCount, @NotBlank String endReason,
            Map<String, Object> customState) {}
    public record SessionsData(List<SessionData> sessions) {}
    public record SessionData(long sessionId, String malangiType, String status, String contextType,
            LocalDateTime startedAt, LocalDateTime endedAt, int durationSeconds, int totalInteractionCount,
            String endReason, Map<String, Object> customState) {
        static SessionData from(MalangiSession s) { return from(s, Map.of()); }
        static SessionData from(MalangiUseCase.StartedSession started) { return from(started.session(), started.customState()); }
        private static SessionData from(MalangiSession s, Map<String, Object> state) { return new SessionData(s.id(), s.malangiType().name(), s.status().name(),
                s.contextType().name(), s.startedAt(), s.endedAt(), s.durationSeconds(), s.totalInteractionCount(),
                s.endReason() == null ? null : s.endReason().name(), state); }
    }
    public record EndData(long sessionId, String malangiType, String status, int durationSeconds,
            int touchCount, int longPressCount, int dragCount, int totalInteractionCount, String endReason, int totalPlayTime) {
        static EndData from(MalangiUseCase.CompletedSession result) { var s = result.session(); return new EndData(s.id(),
                s.malangiType().name(), s.status().name(), s.durationSeconds(), s.touchCount(), s.longPressCount(),
                s.dragCount(), s.totalInteractionCount(), s.endReason().name(), result.totalPlayTime()); }
    }
}
