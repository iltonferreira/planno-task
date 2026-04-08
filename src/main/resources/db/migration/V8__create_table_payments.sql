CREATE TABLE IF NOT EXISTS payments (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    amount DOUBLE PRECISION NOT NULL,
    type VARCHAR(50) NOT NULL,
    direction VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    due_date DATE,
    paid_at TIMESTAMP,
    external_reference VARCHAR(255),
    external_payment_id VARCHAR(255),
    external_preference_id VARCHAR(255),
    external_subscription_id VARCHAR(255),
    checkout_url VARCHAR(1000),
    status_detail VARCHAR(1000),
    client_id BIGINT,
    project_id BIGINT,
    subscription_id BIGINT,
    created_by_user_id BIGINT,
    tenant_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_payments_client FOREIGN KEY (client_id) REFERENCES clients (id),
    CONSTRAINT fk_payments_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_payments_subscription FOREIGN KEY (subscription_id) REFERENCES subscriptions (id),
    CONSTRAINT fk_payments_created_by FOREIGN KEY (created_by_user_id) REFERENCES users (id),
    CONSTRAINT fk_payments_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
);

CREATE INDEX IF NOT EXISTS idx_payments_tenant ON payments (tenant_id);
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments (status);
CREATE INDEX IF NOT EXISTS idx_payments_external_payment_id ON payments (external_payment_id);
