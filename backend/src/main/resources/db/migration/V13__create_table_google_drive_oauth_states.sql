CREATE TABLE IF NOT EXISTS google_drive_oauth_states (
    id BIGSERIAL PRIMARY KEY,
    state VARCHAR(255) NOT NULL UNIQUE,
    tenant_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_google_drive_oauth_states_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_google_drive_oauth_states_state ON google_drive_oauth_states(state);
CREATE INDEX IF NOT EXISTS idx_google_drive_oauth_states_expires_at ON google_drive_oauth_states(expires_at);
