CREATE TABLE IF NOT EXISTS subscriptions (
    id BIGSERIAL PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    price DOUBLE PRECISION NOT NULL,
    status VARCHAR(50),
    next_billing_date DATE,
    external_reference VARCHAR(255),
    client_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    CONSTRAINT fk_subscriptions_client FOREIGN KEY (client_id) REFERENCES clients(id),
    CONSTRAINT fk_subscriptions_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE INDEX IF NOT EXISTS idx_subscriptions_client_id ON subscriptions(client_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_tenant_id ON subscriptions(tenant_id);
