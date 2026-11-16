ALTER TABLE counseling_results ADD COLUMN is_hidden BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE counseling_reports (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    period_year INT NOT NULL,
    period_month INT NOT NULL,
    source_signature VARCHAR(100) NOT NULL,
    content JSON NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_counseling_reports PRIMARY KEY (id),
    CONSTRAINT uk_reports_user_period UNIQUE (user_id, period_year, period_month),
    CONSTRAINT fk_counseling_reports_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
