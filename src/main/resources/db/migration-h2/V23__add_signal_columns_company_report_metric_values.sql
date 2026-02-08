ALTER TABLE company_report_metric_values ADD COLUMN signal_color VARCHAR(10);
ALTER TABLE company_report_metric_values ADD COLUMN color_rationale VARCHAR(255);
ALTER TABLE company_report_metric_values ADD COLUMN benchmark_value DECIMAL(20,4);

CREATE INDEX idx_crmv_signal_lookup ON company_report_metric_values(report_version_id, signal_color, metric_id);
CREATE INDEX idx_crmv_color_filter ON company_report_metric_values(signal_color, quarter_id, deleted_at);

ALTER TABLE company_report_metric_values
  ADD CONSTRAINT chk_crmv_signal_color CHECK (signal_color IN ('GREEN','YELLOW','RED'));
