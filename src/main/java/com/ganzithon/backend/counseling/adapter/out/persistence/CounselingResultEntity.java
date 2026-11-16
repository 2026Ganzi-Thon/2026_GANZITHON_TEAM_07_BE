package com.ganzithon.backend.counseling.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "counseling_results")
class CounselingResultEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "situation", columnDefinition = "TEXT")
    private String situation;

    @Column(name = "primary_emotion", length = 50)
    private String primaryEmotion;

    @Column(name = "pre_intensity")
    private Integer preIntensity;

    @Column(name = "post_intensity")
    private Integer postIntensity;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "recommended_malangi", length = 50)
    private String recommendedMalangi;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "timeline", nullable = false, columnDefinition = "json")
    private List<Map<String, Object>> timeline = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_hidden", nullable = false)
    private boolean hidden;

    protected CounselingResultEntity() {}

    CounselingResultEntity(long sessionId, long userId, String situation, String primaryEmotion, Integer preIntensity,
            Integer postIntensity, String summary, String recommendedMalangi, List<Map<String, Object>> timeline,
            LocalDateTime createdAt) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.situation = situation;
        this.primaryEmotion = primaryEmotion;
        this.preIntensity = preIntensity;
        this.postIntensity = postIntensity;
        this.summary = summary;
        this.recommendedMalangi = recommendedMalangi;
        this.timeline = new ArrayList<>(timeline);
        this.createdAt = createdAt;
    }

    Long id() { return id; }
    Long sessionId() { return sessionId; }
    String situation() { return situation; }
    String primaryEmotion() { return primaryEmotion; }
    Integer preIntensity() { return preIntensity; }
    Integer postIntensity() { return postIntensity; }
    String summary() { return summary; }
    String recommendedMalangi() { return recommendedMalangi; }
    List<Map<String, Object>> timeline() { return timeline; }
    LocalDateTime createdAt() { return createdAt; }
    boolean hidden() { return hidden; }
    void hide(boolean hidden) { this.hidden = hidden; }
}
