-- refresh/email verification 토큰 평문 저장 제거를 위한 해시 컬럼 전환

ALTER TABLE refresh_tokens
    ADD COLUMN token_hash VARCHAR(64) NULL COMMENT '리프레시 토큰 SHA-256 해시' AFTER token_value;

UPDATE refresh_tokens
SET token_hash = LOWER(SHA2(token_value, 256))
WHERE token_hash IS NULL
  AND token_value IS NOT NULL;

ALTER TABLE refresh_tokens
    MODIFY token_value VARCHAR(512) NULL COMMENT '레거시 리프레시 토큰 평문(마이그레이션 후 미사용)';

UPDATE refresh_tokens
SET token_value = NULL
WHERE token_hash IS NOT NULL;

ALTER TABLE refresh_tokens
    ADD UNIQUE KEY uk_refresh_token_hash (token_hash);

ALTER TABLE email_verifications
    ADD COLUMN token_hash VARCHAR(64) NULL COMMENT '이메일 인증 토큰 SHA-256 해시' AFTER token;

UPDATE email_verifications
SET token_hash = LOWER(SHA2(token, 256))
WHERE token_hash IS NULL
  AND token IS NOT NULL;

ALTER TABLE email_verifications
    MODIFY token VARCHAR(255) NULL COMMENT '레거시 인증 토큰 평문(마이그레이션 후 미사용)';

UPDATE email_verifications
SET token = NULL
WHERE token_hash IS NOT NULL;

ALTER TABLE email_verifications
    ADD UNIQUE KEY uk_ev_token_hash (token_hash);
