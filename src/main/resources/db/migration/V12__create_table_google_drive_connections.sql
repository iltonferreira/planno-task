CREATE TABLE IF NOT EXISTS google_drive_connections (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL UNIQUE,
    google_account_email VARCHAR(255) NOT NULL,
    access_token TEXT,
    refresh_token TEXT NOT NULL,
    token_type VARCHAR(50),
    scope TEXT,
    expires_at TIMESTAMP,
    root_folder_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_google_drive_connections_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_google_drive_connections_tenant_id ON google_drive_connections(tenant_id);
