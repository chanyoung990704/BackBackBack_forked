CREATE TABLE `report_analyses` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `company_id` BIGINT NOT NULL COMMENT '기업 ID',
  `company_name` VARCHAR(255) NOT NULL COMMENT '기업명 (캐시)',
  `total_count` INT NOT NULL DEFAULT 0 COMMENT '분석된 항목 수',
  `average_score` DECIMAL(10,6) NULL COMMENT '평균 점수',
  `analyzed_at` TIMESTAMP NOT NULL COMMENT 'AI 분석 시각 (UTC)',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` TIMESTAMP NULL,
  `created_by` BIGINT NULL,
  `updated_by` BIGINT NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_report_analysis_company` (`company_id`, `analyzed_at` DESC),
  INDEX `idx_report_analysis_timestamp` (`analyzed_at`),
  CONSTRAINT `fk_report_analysis_company` FOREIGN KEY (`company_id`)
    REFERENCES `companies`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='사업보고서 분석 세션';

CREATE TABLE `report_contents` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `report_analysis_id` BIGINT NOT NULL COMMENT '분석 세션 ID',
  `title` VARCHAR(500) NOT NULL COMMENT '보고서 항목 제목',
  `summary` TEXT NULL COMMENT '요약',
  `score` DECIMAL(10,6) NULL COMMENT '점수',
  `published_at` TIMESTAMP NULL COMMENT '발행 시각',
  `link` VARCHAR(2000) NULL COMMENT '원문 URL',
  `sentiment` VARCHAR(10) NULL COMMENT '감성 분류 (POS/NEU/NEG)',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` TIMESTAMP NULL,
  `created_by` BIGINT NULL,
  `updated_by` BIGINT NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_report_content_analysis` (`report_analysis_id`),
  INDEX `idx_report_content_published` (`published_at` DESC),
  CONSTRAINT `fk_report_content_analysis` FOREIGN KEY (`report_analysis_id`)
    REFERENCES `report_analyses`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='사업보고서 요약 항목';
