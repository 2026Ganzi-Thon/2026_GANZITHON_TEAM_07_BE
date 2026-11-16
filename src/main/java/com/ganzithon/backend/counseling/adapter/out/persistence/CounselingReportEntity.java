package com.ganzithon.backend.counseling.adapter.out.persistence;

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

/** 월간 분석 레포트 캐시 (V5). 그 달 기록이 바뀌면(signature) 다시 만든다. */
@Entity
@Table(name = "counseling_reports")
class CounselingReportEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "period_year", nullable = false)
    private int periodYear;

    @Column(name = "period_month", nullable = false)
    private int periodMonth;

    @Column(name = "source_signature", nullable = false, length = 100)
    private String sourceSignature;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content", nullable = false, columnDefinition = "json")
    private Map<String, Object> content = new LinkedHashMap<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected CounselingReportEntity() {}

    CounselingReportEntity(long userId, int periodYear, int periodMonth) {
        this.userId = userId;
        this.periodYear = periodYear;
        this.periodMonth = periodMonth;
    }

    void update(String signature, Map<String, Object> content, LocalDateTime now) {
        this.sourceSignature = signature;
        this.content = new LinkedHashMap<>(content);
        this.createdAt = now;
    }

    int periodYear() { return periodYear; }
    int periodMonth() { return periodMonth; }
    String sourceSignature() { return sourceSignature; }
    Map<String, Object> content() { return content; }
    LocalDateTime createdAt() { return createdAt; }
}
