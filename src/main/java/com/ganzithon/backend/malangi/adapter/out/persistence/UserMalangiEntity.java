package com.ganzithon.backend.malangi.adapter.out.persistence;

import com.ganzithon.backend.malangi.domain.MalangiType;
import com.ganzithon.backend.malangi.domain.OwnedMalangi;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.LinkedHashMap;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_malangis", uniqueConstraints =
        @UniqueConstraint(name = "uk_user_malangi", columnNames = {"user_id", "malangi_type"}))
class UserMalangiEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "malangi_type", nullable = false, length = 50)
    private String malangiType;

    @Column(name = "is_unlocked", nullable = false)
    private boolean unlocked;

    @Column(name = "is_selected", nullable = false)
    private boolean selected;

    @Column(name = "total_play_time", nullable = false)
    private int totalPlayTime;

    @Column(name = "last_interacted_at")
    private LocalDateTime lastInteractedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_state", nullable = false, columnDefinition = "json")
    private Map<String, Object> customState = new LinkedHashMap<>();

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected UserMalangiEntity() {}

    UserMalangiEntity(long userId, MalangiType type, boolean unlocked, boolean selected) {
        this.userId = userId;
        this.malangiType = type.name();
        this.unlocked = unlocked;
        this.selected = selected;
    }

    OwnedMalangi toDomain() {
        return new OwnedMalangi(MalangiType.valueOf(malangiType), unlocked, selected, totalPlayTime,
                lastInteractedAt, customState);
    }

    MalangiType type() {
        return MalangiType.valueOf(malangiType);
    }

    boolean unlocked() {
        return unlocked;
    }

    void select(boolean selected) {
        this.selected = selected;
    }

    Map<String, Object> customState() { return customState; }

    void updateCustomState(Map<String, Object> customState) { this.customState = new LinkedHashMap<>(customState); }

    void recordPlay(int durationSeconds, LocalDateTime endedAt) {
        this.totalPlayTime += durationSeconds;
        this.lastInteractedAt = endedAt;
    }

    int totalPlayTime() { return totalPlayTime; }
}
