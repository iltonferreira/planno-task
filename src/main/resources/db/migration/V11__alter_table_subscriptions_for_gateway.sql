ALTER TABLE subscriptions
    ADD COLUMN IF NOT EXISTS external_subscription_id VARCHAR(255);

ALTER TABLE subscriptions
    ADD COLUMN IF NOT EXISTS checkout_url VARCHAR(1000);
