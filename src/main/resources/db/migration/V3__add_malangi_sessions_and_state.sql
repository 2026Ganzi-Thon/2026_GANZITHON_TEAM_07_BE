ALTER TABLE user_malangis ADD COLUMN custom_state JSON NOT NULL DEFAULT (JSON_OBJECT());

CREATE TABLE malangi_sessions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    malangi_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    context_type VARCHAR(30) NOT NULL DEFAULT 'MAIN',
    started_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ended_at DATETIME(6) NULL,
    duration_seconds INT NOT NULL DEFAULT 0,
    touch_count INT NOT NULL DEFAULT 0,
    long_press_count INT NOT NULL DEFAULT 0,
    drag_count INT NOT NULL DEFAULT 0,
    total_interaction_count INT NOT NULL DEFAULT 0,
    end_reason VARCHAR(30) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_malangi_sessions PRIMARY KEY (id),
    CONSTRAINT fk_malangi_sessions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_malangi_user_started ON malangi_sessions (user_id, started_at DESC);
CREATE INDEX idx_malangi_user_status ON malangi_sessions (user_id, status);
