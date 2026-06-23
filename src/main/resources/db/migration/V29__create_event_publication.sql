CREATE TABLE IF NOT EXISTS event_publication (
    id BINARY(16) NOT NULL,
    completion_date DATETIME(6) NULL,
    event_type VARCHAR(512) NOT NULL,
    listener_id VARCHAR(512) NOT NULL,
    publication_date DATETIME(6) NOT NULL,
    serialized_event VARCHAR(4000) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
