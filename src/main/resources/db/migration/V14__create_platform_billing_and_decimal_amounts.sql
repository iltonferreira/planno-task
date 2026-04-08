ALTER TABLE subscriptions
    ALTER COLUMN price TYPE NUMERIC(12, 2) USING ROUND(price::numeric, 2);

ALTER TABLE payments
    ALTER COLUMN amount TYPE NUMERIC(12, 2) USING ROUND(amount::numeric, 2);

CREATE TABLE IF NOT EXISTS platform_subscriptions (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL UNIQUE,
    plan_code VARCHAR(100) NOT NULL,
    plan_name VARCHAR(255) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    currency_id VARCHAR(10) NOT NULL,
    payer_email VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    external_reference VARCHAR(255) UNIQUE,
    external_subscription_id VARCHAR(255),
    checkout_url VARCHAR(1000),
    next_billing_date DATE,
    last_payment_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_platform_subscriptions_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE INDEX IF NOT EXISTS idx_platform_subscriptions_status ON platform_subscriptions(status);
CREATE INDEX IF NOT EXISTS idx_platform_subscriptions_external_subscription_id ON platform_subscriptions(external_subscription_id);
