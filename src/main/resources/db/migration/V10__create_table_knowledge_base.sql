CREATE TABLE IF NOT EXISTS knowledge_base_pages (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    summary TEXT,
    content TEXT NOT NULL,
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    created_by_user_id BIGINT NOT NULL,
    updated_by_user_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_kb_created_by FOREIGN KEY (created_by_user_id) REFERENCES users (id),
    CONSTRAINT fk_kb_updated_by FOREIGN KEY (updated_by_user_id) REFERENCES users (id),
    CONSTRAINT fk_kb_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT uk_kb_tenant_slug UNIQUE (tenant_id, slug)
);

CREATE INDEX IF NOT EXISTS idx_kb_tenant_updated_at ON knowledge_base_pages (tenant_id, updated_at DESC);
