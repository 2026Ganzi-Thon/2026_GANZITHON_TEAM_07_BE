package com.ganzithon.backend.counseling.adapter.out.persistence;

import com.ganzithon.backend.counseling.domain.CounselingSession;
import com.ganzithon.backend.counseling.domain.CounselingState;
import com.ganzithon.backend.counseling.domain.CounselingStatus;
import com.ganzithon.backend.counseling.domain.CounselingStep;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "counseling_sessions")
class CounselingSessionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "malangi_session_id")
    private Long malangiSessionId;

    @Column(name = "current_step", nullable = false, length = 20)
    private String currentStep;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "risk_flag", nullable = false)
    private boolean riskFlag;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "session_state", nullable = false, columnDefinition = "json")
    private Map<String, Object> sessionState = new LinkedHashMap<>();

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    protected CounselingSessionEntity() {}

    CounselingSessionEntity(long userId, Long malangiSessionId, LocalDateTime startedAt) {
        this.userId = userId;
        this.malangiSessionId = malangiSessionId;
        this.currentStep = CounselingStep.STEP_01.name();
        this.status = CounselingStatus.IN_PROGRESS.name();
        this.sessionState = new CounselingState().toMap();
        this.startedAt = startedAt;
    }

    CounselingSession toDomain() {
        return new CounselingSession(id, userId, malangiSessionId, CounselingStep.valueOf(currentStep),
                CounselingStatus.valueOf(status), riskFlag, CounselingState.fromMap(sessionState), startedAt, endedAt);
    }

    void update(CounselingSession session) {
        this.currentStep = session.step().name();
        this.status = session.status().name();
        this.riskFlag = session.riskFlag();
        this.sessionState = new LinkedHashMap<>(session.state().toMap());
        this.endedAt = session.endedAt();
    }
}
