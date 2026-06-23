ALTER TABLE saga_instance ADD COLUMN deleted_at TIMESTAMP NULL;
ALTER TABLE saga_instance ADD COLUMN created_by BIGINT NULL;
ALTER TABLE saga_instance ADD COLUMN updated_by BIGINT NULL;
