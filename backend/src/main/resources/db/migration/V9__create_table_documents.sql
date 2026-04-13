CREATE TABLE IF NOT EXISTS document_assets (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(255) NOT NULL,
    file_size BIGINT,
    storage_file_id VARCHAR(255) NOT NULL UNIQUE,
    storage_folder_id VARCHAR(255),
    storage_folder_path VARCHAR(500) NOT NULL,
    web_view_url VARCHAR(1000),
    relation_type VARCHAR(50) NOT NULL,
    relation_id BIGINT,
    uploaded_by_user_id BIGINT,
    tenant_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_documents_uploaded_by FOREIGN KEY (uploaded_by_user_id) REFERENCES users (id),
    CONSTRAINT fk_documents_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
);

CREATE INDEX IF NOT EXISTS idx_documents_tenant ON document_assets (tenant_id);
CREATE INDEX IF NOT EXISTS idx_documents_relation ON document_assets (tenant_id, relation_type, relation_id);
