CREATE TABLE outbox_event (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type   VARCHAR(100) NOT NULL,
    payload      JSON NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at   DATETIME(6) NOT NULL,
    processed_at DATETIME(6) NULL,
    INDEX idx_outbox_event_status_id (status, id)
);
