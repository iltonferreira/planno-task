CREATE TABLE IF NOT EXISTS clients (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(255),
    document VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at TIMESTAMP,
    tenant_id BIGINT NOT NULL,
    CONSTRAINT fk_clients_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE INDEX IF NOT EXISTS idx_clients_tenant_id ON clients(tenant_id);
