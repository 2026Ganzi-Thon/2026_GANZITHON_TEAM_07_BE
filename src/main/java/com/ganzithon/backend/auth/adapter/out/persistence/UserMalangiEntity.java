package com.ganzithon.backend.auth.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected UserMalangiEntity() {}

    UserMalangiEntity(long userId, String malangiType, boolean unlocked, boolean selected) {
        this.userId = userId;
        this.malangiType = malangiType;
        this.unlocked = unlocked;
        this.selected = selected;
    }
}
