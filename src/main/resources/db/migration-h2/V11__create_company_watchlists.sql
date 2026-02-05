CREATE TABLE company_watchlists (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  company_id BIGINT NOT NULL,
  note VARCHAR(255),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP,
  created_by BIGINT,
  updated_by BIGINT,
  CONSTRAINT uk_user_company UNIQUE (user_id, company_id),
  CONSTRAINT fk_cw_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_cw_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE
);
CREATE INDEX idx_cw_user ON company_watchlists(user_id);
CREATE INDEX idx_cw_company ON company_watchlists(company_id);
CREATE INDEX idx_cw_user_deleted ON company_watchlists(user_id, deleted_at);
