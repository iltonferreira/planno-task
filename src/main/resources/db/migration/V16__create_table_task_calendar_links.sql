CREATE TABLE task_calendar_links (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    calendar_id VARCHAR(255) NOT NULL DEFAULT 'primary',
    external_event_id VARCHAR(255) NOT NULL,
    imported_from_google BOOLEAN NOT NULL DEFAULT FALSE,
    last_synced_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_task_calendar_link_task_user UNIQUE (task_id, user_id),
    CONSTRAINT uk_task_calendar_link_user_event UNIQUE (user_id, calendar_id, external_event_id)
);

CREATE INDEX idx_task_calendar_links_task_id
    ON task_calendar_links(task_id);

CREATE INDEX idx_task_calendar_links_user_id
    ON task_calendar_links(user_id);
