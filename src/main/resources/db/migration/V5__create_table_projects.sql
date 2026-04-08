CREATE TABLE IF NOT EXISTS projects (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL,
    budget DOUBLE PRECISION,
    start_date DATE,
    end_date DATE,
    client_id BIGINT,
    owner_user_id BIGINT,
    created_by_user_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_projects_client FOREIGN KEY (client_id) REFERENCES clients(id),
    CONSTRAINT fk_projects_owner_user FOREIGN KEY (owner_user_id) REFERENCES users(id),
    CONSTRAINT fk_projects_created_by_user FOREIGN KEY (created_by_user_id) REFERENCES users(id),
    CONSTRAINT fk_projects_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE INDEX IF NOT EXISTS idx_projects_tenant_id ON projects(tenant_id);
CREATE INDEX IF NOT EXISTS idx_projects_client_id ON projects(client_id);
