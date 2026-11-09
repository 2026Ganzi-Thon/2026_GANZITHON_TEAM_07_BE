package com.ganzithon.backend.malangi.domain;

import java.time.LocalDateTime;

public record MalangiSession(long id, MalangiType malangiType, Status status, ContextType contextType,
        LocalDateTime startedAt, LocalDateTime endedAt, int durationSeconds, int touchCount,
        int longPressCount, int dragCount, EndReason endReason) {
    public int totalInteractionCount() { return touchCount + longPressCount + dragCount; }
    public enum Status { IN_PROGRESS, COMPLETED }
    public enum ContextType { MAIN, AFTER_COUNSELING, SLEEP_MODE }
    public enum EndReason { EXIT, TO_COUNSELING, TO_SLEEP }
}
