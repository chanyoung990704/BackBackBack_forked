CREATE TABLE metric_averages (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  quarter_id BIGINT NOT NULL,
  metric_id BIGINT NOT NULL,
  avg_value DECIMAL(20,4),
  median_value DECIMAL(20,4),
  min_value DECIMAL(20,4),
  max_value DECIMAL(20,4),
  stddev_value DECIMAL(20,4),
  company_count INT NOT NULL DEFAULT 0,
  calculated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  data_source_version INT NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP,
  created_by BIGINT,
  updated_by BIGINT,
  CONSTRAINT uk_quarter_metric UNIQUE (quarter_id, metric_id),
  CONSTRAINT fk_ma_quarter FOREIGN KEY (quarter_id) REFERENCES quarters(id) ON DELETE CASCADE,
  CONSTRAINT fk_ma_metric FOREIGN KEY (metric_id) REFERENCES metrics(id) ON DELETE CASCADE
);
CREATE INDEX idx_ma_quarter ON metric_averages (quarter_id);
CREATE INDEX idx_ma_metric ON metric_averages (metric_id);
