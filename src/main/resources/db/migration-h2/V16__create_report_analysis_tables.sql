CREATE TABLE report_analyses (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  company_id BIGINT NOT NULL,
  company_name VARCHAR(255) NOT NULL,
  total_count INT DEFAULT 0 NOT NULL,
  average_score DECIMAL(10,6),
  analyzed_at TIMESTAMP NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  deleted_at TIMESTAMP,
  created_by BIGINT,
  updated_by BIGINT,
  CONSTRAINT fk_report_analysis_company FOREIGN KEY (company_id)
    REFERENCES companies(id) ON DELETE CASCADE
);

CREATE INDEX idx_report_analysis_company ON report_analyses(company_id, analyzed_at DESC);
CREATE INDEX idx_report_analysis_timestamp ON report_analyses(analyzed_at);

CREATE TABLE report_contents (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  report_analysis_id BIGINT NOT NULL,
  title VARCHAR(500) NOT NULL,
  summary TEXT,
  score DECIMAL(10,6),
  published_at TIMESTAMP,
  link VARCHAR(2000),
  sentiment VARCHAR(10),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  deleted_at TIMESTAMP,
  created_by BIGINT,
  updated_by BIGINT,
  CONSTRAINT fk_report_content_analysis FOREIGN KEY (report_analysis_id)
    REFERENCES report_analyses(id) ON DELETE CASCADE
);

CREATE INDEX idx_report_content_analysis ON report_contents(report_analysis_id);
CREATE INDEX idx_report_content_published ON report_contents(published_at DESC);
