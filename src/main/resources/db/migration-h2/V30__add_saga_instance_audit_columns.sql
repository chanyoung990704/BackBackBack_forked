ALTER TABLE saga_instance ADD COLUMN deleted_at TIMESTAMP;
ALTER TABLE saga_instance ADD COLUMN created_by BIGINT;
ALTER TABLE saga_instance ADD COLUMN updated_by BIGINT;
