package com.ganzithon.backend.malangi.adapter.out.persistence;

import com.ganzithon.backend.malangi.domain.MalangiSession;
import com.ganzithon.backend.malangi.domain.MalangiSession.ContextType;
import com.ganzithon.backend.malangi.domain.MalangiSession.EndReason;
import com.ganzithon.backend.malangi.domain.MalangiSession.Status;
import com.ganzithon.backend.malangi.domain.MalangiType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "malangi_sessions")
class MalangiSessionEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "malangi_type", nullable = false) private String malangiType;
    @Column(nullable = false) private String status;
    @Column(name = "context_type", nullable = false) private String contextType;
    @Column(name = "started_at", nullable = false) private LocalDateTime startedAt;
    @Column(name = "ended_at") private LocalDateTime endedAt;
    @Column(name = "duration_seconds", nullable = false) private int durationSeconds;
    @Column(name = "touch_count", nullable = false) private int touchCount;
    @Column(name = "long_press_count", nullable = false) private int longPressCount;
    @Column(name = "drag_count", nullable = false) private int dragCount;
    @Column(name = "total_interaction_count", nullable = false) private int totalInteractionCount;
    @Column(name = "end_reason") private String endReason;

    protected MalangiSessionEntity() {}
    MalangiSessionEntity(long userId, MalangiType type, ContextType contextType, LocalDateTime startedAt) {
        this.userId = userId; this.malangiType = type.name(); this.contextType = contextType.name();
        this.status = Status.IN_PROGRESS.name(); this.startedAt = startedAt;
    }
    void complete(int duration, int touch, int press, int drag, EndReason reason, LocalDateTime endedAt) {
        this.status = Status.COMPLETED.name(); this.durationSeconds = duration; this.touchCount = touch;
        this.longPressCount = press; this.dragCount = drag; this.totalInteractionCount = touch + press + drag;
        this.endReason = reason.name(); this.endedAt = endedAt;
    }
    MalangiSession toDomain() { return new MalangiSession(id, MalangiType.valueOf(malangiType), Status.valueOf(status),
            ContextType.valueOf(contextType), startedAt, endedAt, durationSeconds, touchCount, longPressCount,
            dragCount, endReason == null ? null : EndReason.valueOf(endReason)); }
    long userId() { return userId; }
    MalangiType type() { return MalangiType.valueOf(malangiType); }
    boolean inProgress() { return Status.IN_PROGRESS.name().equals(status); }
}
