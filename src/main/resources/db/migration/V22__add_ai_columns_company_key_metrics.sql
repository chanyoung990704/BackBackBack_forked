ALTER TABLE `company_key_metrics`
  ADD COLUMN `ai_comment` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL
    COMMENT 'AI 종합 분석 코멘트 (Plain text, 섹션 구분자 포함)',
  ADD COLUMN `ai_sections` JSON NULL
    COMMENT 'AI 분석 섹션별 구조화 데이터 (앱에서 파싱하여 렌더링)',
  ADD COLUMN `ai_model_version` VARCHAR(20) NULL
    COMMENT 'AI 모델 버전 (예: finbert-v2.1, gpt-4-turbo)',
  ADD COLUMN `ai_prompt_hash` VARCHAR(64) NULL
    COMMENT '프롬프트 해시값 (재현성/캐싱용)',
  ADD COLUMN `ai_analyzed_at` TIMESTAMP NULL
    COMMENT 'AI 분석 시각',
  ADD KEY `idx_ckm_ai_analyzed` (`ai_analyzed_at`, `deleted_at`),
  ADD KEY `idx_ckm_model_ver` (`ai_model_version`);
