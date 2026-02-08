ALTER TABLE `company_report_metric_values`
  ADD COLUMN `signal_color` ENUM('GREEN','YELLOW','RED') NULL
    COMMENT '지표 신호등 상태',
  ADD COLUMN `color_rationale` VARCHAR(255) NULL
    COMMENT '색상 판정 근거 (예: 업종평균 대비 -15%p)',
  ADD COLUMN `benchmark_value` DECIMAL(20,4) NULL
    COMMENT '비교 기준값 (업종평균/직전분기 등)',
  ADD KEY `idx_crmv_signal_lookup` (`report_version_id`, `signal_color`, `metric_id`),
  ADD KEY `idx_crmv_color_filter` (`signal_color`, `quarter_id`, `deleted_at`);
