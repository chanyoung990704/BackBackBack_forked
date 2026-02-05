CREATE TABLE risk_score_summaries (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  company_id BIGINT NOT NULL,
  quarter_id BIGINT NOT NULL,
  report_version_id BIGINT NOT NULL,
  risk_score DECIMAL(6,2),
  risk_level VARCHAR(20) NOT NULL,
  risk_metrics_count INT NOT NULL DEFAULT 0,
  risk_metrics_avg DECIMAL(6,2),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP,
  created_by BIGINT,
  updated_by BIGINT,
  calculated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_refreshed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_company_quarter_version UNIQUE (company_id, quarter_id, report_version_id),
  CONSTRAINT fk_rss_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
  CONSTRAINT fk_rss_quarter FOREIGN KEY (quarter_id) REFERENCES quarters(id) ON DELETE CASCADE,
  CONSTRAINT fk_rss_report_version FOREIGN KEY (report_version_id) REFERENCES company_report_versions(id) ON DELETE CASCADE,
  CONSTRAINT ck_rss_risk_level CHECK (risk_level IN ('SAFE', 'CAUTION', 'DANGER', 'UNDEFINED'))
);

CREATE INDEX idx_rss_quarter ON risk_score_summaries (quarter_id, risk_level);
CREATE INDEX idx_rss_company_level ON risk_score_summaries (company_id, risk_level);
