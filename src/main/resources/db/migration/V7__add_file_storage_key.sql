ALTER TABLE files
ADD COLUMN storage_key VARCHAR(500) NULL COMMENT '스토리지 키(S3 key 등)';
