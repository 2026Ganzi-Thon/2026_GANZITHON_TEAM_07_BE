package com.ganzithon.backend.counseling.domain;

import java.time.LocalDateTime;

/** 상담 세션 (counseling_sessions). 단계와 상태는 서버가 관리한다 (01번 이슈). */
public class CounselingSession {
    private final long id;
    private final long userId;
    private final Long malangiSessionId;
    private CounselingStep step;
    private CounselingStatus status;
    private boolean riskFlag;
    private final CounselingState state;
    private final LocalDateTime startedAt;
    private LocalDateTime endedAt;

    public CounselingSession(long id, long userId, Long malangiSessionId, CounselingStep step, CounselingStatus status,
            boolean riskFlag, CounselingState state, LocalDateTime startedAt, LocalDateTime endedAt) {
        this.id = id;
        this.userId = userId;
        this.malangiSessionId = malangiSessionId;
        this.step = step;
        this.status = status;
        this.riskFlag = riskFlag;
        this.state = state;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
    }

    public long id() { return id; }
    public long userId() { return userId; }
    public Long malangiSessionId() { return malangiSessionId; }
    public CounselingStep step() { return step; }
    public CounselingStatus status() { return status; }
    public boolean riskFlag() { return riskFlag; }
    public CounselingState state() { return state; }
    public LocalDateTime startedAt() { return startedAt; }
    public LocalDateTime endedAt() { return endedAt; }

    public boolean inProgress() {
        return status == CounselingStatus.IN_PROGRESS;
    }

    /** 단계를 옮기고 그 단계의 질문 기록을 새로 시작한다. */
    public void moveTo(CounselingStep next) {
        this.step = next;
        state.resetStepProgress();
    }

    /** 위기 감지: 진행을 멈추고 결과를 만들지 않는다 (features.md F-09). 단계는 감지 당시 그대로. */
    public void abortByRisk(LocalDateTime now) {
        this.riskFlag = true;
        this.status = CounselingStatus.ABORTED_RISK;
        this.endedAt = now;
        state.nextInput(NextInput.NONE);
    }

    public void abortByUser(LocalDateTime now) {
        this.status = CounselingStatus.ABORTED_USER;
        this.endedAt = now;
        state.nextInput(NextInput.NONE);
    }

    public void complete(LocalDateTime now) {
        this.step = CounselingStep.STEP_06;
        this.status = CounselingStatus.COMPLETED;
        this.endedAt = now;
        state.nextInput(NextInput.NONE);
    }
}
