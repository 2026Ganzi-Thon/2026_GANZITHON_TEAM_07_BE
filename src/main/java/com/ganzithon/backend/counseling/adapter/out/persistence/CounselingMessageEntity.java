package com.ganzithon.backend.counseling.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "counseling_messages")
class CounselingMessageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "step", nullable = false, length = 20)
    private String step;

    @Column(name = "sender_type", nullable = false, length = 20)
    private String senderType;

    @Column(name = "message_content", nullable = false, columnDefinition = "TEXT")
    private String messageContent;

    @Column(name = "is_risk_detected", nullable = false)
    private boolean riskDetected;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected CounselingMessageEntity() {}

    CounselingMessageEntity(long sessionId, String step, String senderType, String messageContent,
            boolean riskDetected, LocalDateTime createdAt) {
        this.sessionId = sessionId;
        this.step = step;
        this.senderType = senderType;
        this.messageContent = messageContent;
        this.riskDetected = riskDetected;
        this.createdAt = createdAt;
    }

    Long id() { return id; }
    String senderType() { return senderType; }
    String messageContent() { return messageContent; }
    String step() { return step; }
    LocalDateTime createdAt() { return createdAt; }

    void markRisk() {
        this.riskDetected = true;
    }
}
