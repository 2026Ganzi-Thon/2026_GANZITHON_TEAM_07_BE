CREATE TABLE counseling_sessions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    malangi_session_id BIGINT NULL,
    current_step VARCHAR(20) NOT NULL DEFAULT 'STEP_01',
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    risk_flag BOOLEAN NOT NULL DEFAULT FALSE,
    session_state JSON NOT NULL DEFAULT (JSON_OBJECT()),
    started_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ended_at DATETIME(6) NULL,
    CONSTRAINT pk_counseling_sessions PRIMARY KEY (id),
    CONSTRAINT fk_counseling_sessions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_counseling_sessions_malangi_session FOREIGN KEY (malangi_session_id) REFERENCES malangi_sessions (id) ON DELETE SET NULL
);

CREATE INDEX idx_sessions_user_status ON counseling_sessions (user_id, status);
CREATE INDEX idx_sessions_malangi ON counseling_sessions (malangi_session_id);

CREATE TABLE counseling_messages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    step VARCHAR(20) NOT NULL DEFAULT 'STEP_01',
    sender_type VARCHAR(20) NOT NULL,
    message_content TEXT NOT NULL,
    is_risk_detected BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_counseling_messages PRIMARY KEY (id),
    CONSTRAINT fk_counseling_messages_session FOREIGN KEY (session_id) REFERENCES counseling_sessions (id) ON DELETE CASCADE
);

CREATE INDEX idx_messages_session_id ON counseling_messages (session_id, created_at ASC);

CREATE TABLE counseling_results (
    id BIGINT NOT NULL AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    situation TEXT NULL,
    primary_emotion VARCHAR(50) NULL,
    pre_intensity INT NULL,
    post_intensity INT NULL,
    summary TEXT NULL,
    recommended_malangi VARCHAR(50) NULL,
    timeline JSON NOT NULL DEFAULT (JSON_ARRAY()),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_counseling_results PRIMARY KEY (id),
    CONSTRAINT uk_results_session_id UNIQUE (session_id),
    CONSTRAINT fk_counseling_results_session FOREIGN KEY (session_id) REFERENCES counseling_sessions (id) ON DELETE CASCADE,
    CONSTRAINT fk_counseling_results_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_counseling_results_pre_intensity CHECK (pre_intensity IS NULL OR pre_intensity BETWEEN 0 AND 10),
    CONSTRAINT ck_counseling_results_post_intensity CHECK (post_intensity IS NULL OR post_intensity BETWEEN 0 AND 10)
);

CREATE INDEX idx_results_user_created ON counseling_results (user_id, created_at DESC);
