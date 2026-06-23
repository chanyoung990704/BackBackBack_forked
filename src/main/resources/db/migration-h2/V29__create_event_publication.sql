CREATE TABLE IF NOT EXISTS event_publication (
    id UUID NOT NULL,
    completion_date TIMESTAMP WITH TIME ZONE,
    event_type VARCHAR(512) NOT NULL,
    listener_id VARCHAR(512) NOT NULL,
    publication_date TIMESTAMP WITH TIME ZONE NOT NULL,
    serialized_event VARCHAR(4000) NOT NULL,
    PRIMARY KEY (id)
);
