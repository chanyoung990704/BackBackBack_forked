ALTER TABLE company_key_metrics ADD COLUMN ai_comment VARCHAR(4000);
ALTER TABLE company_key_metrics ADD COLUMN ai_sections JSON;
ALTER TABLE company_key_metrics ADD COLUMN ai_model_version VARCHAR(20);
ALTER TABLE company_key_metrics ADD COLUMN ai_prompt_hash VARCHAR(64);
ALTER TABLE company_key_metrics ADD COLUMN ai_analyzed_at TIMESTAMP;

CREATE INDEX idx_ckm_ai_analyzed ON company_key_metrics(ai_analyzed_at, deleted_at);
CREATE INDEX idx_ckm_model_ver ON company_key_metrics(ai_model_version);
