-- 테스트(H2)용 토큰 해시 컬럼 전환

ALTER TABLE refresh_tokens ADD COLUMN token_hash VARCHAR(64);
ALTER TABLE refresh_tokens ALTER COLUMN token_value VARCHAR(512);
ALTER TABLE refresh_tokens ALTER COLUMN token_value SET NULL;
CREATE UNIQUE INDEX uk_refresh_token_hash ON refresh_tokens (token_hash);

ALTER TABLE email_verifications ADD COLUMN token_hash VARCHAR(64);
ALTER TABLE email_verifications ALTER COLUMN token VARCHAR(255);
ALTER TABLE email_verifications ALTER COLUMN token SET NULL;
CREATE UNIQUE INDEX uk_ev_token_hash ON email_verifications (token_hash);
