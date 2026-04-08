CREATE TABLE google_calendar_connections (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    google_account_email VARCHAR(255) NOT NULL,
    access_token TEXT,
    refresh_token TEXT NOT NULL,
    token_type VARCHAR(120),
    scope TEXT,
    expires_at TIMESTAMP,
    default_calendar_id VARCHAR(255) NOT NULL DEFAULT 'primary',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_google_calendar_connections_user_id
    ON google_calendar_connections(user_id);
