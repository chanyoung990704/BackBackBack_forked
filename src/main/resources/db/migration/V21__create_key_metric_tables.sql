CREATE TABLE `metric_descriptions` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '설명 고유 식별자',
  `metric_id` BIGINT NOT NULL COMMENT '지표 ID (metrics.id)',
  `description` TEXT NOT NULL COMMENT '지표 설명 (계산식 포함)',
  `interpretation` TEXT NULL COMMENT '해석 가이드 (예: 높을수록 안정적)',
  `action_hint` TEXT NULL COMMENT '투자/경영 대응 힌트',
  `locale` VARCHAR(10) NOT NULL DEFAULT 'ko' COMMENT '언어코드 (ko, en)',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` TIMESTAMP NULL COMMENT '삭제일시 (Soft Delete)',
  `created_by` BIGINT NULL,
  `updated_by` BIGINT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_md_metric_locale` (`metric_id`, `locale`),
  CONSTRAINT `fk_md_metric`
    FOREIGN KEY (`metric_id`) REFERENCES `metrics`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='재무지표 상세 설명 (다국어 지원)';

CREATE TABLE `company_key_metrics` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '고유 식별자',
  `company_id` BIGINT NOT NULL COMMENT '기업 ID',
  `quarter_id` BIGINT NOT NULL COMMENT '분기 ID',
  `report_version_id` BIGINT NULL COMMENT '산출 기준 보고서 버전 (추적용)',
  `internal_health_score` DECIMAL(6,2) NULL COMMENT '내부 건강도 (재무/운영)',
  `external_health_score` DECIMAL(6,2) NULL COMMENT '외부 건강도 (시장/뉴스/리포트)',
  `composite_score` DECIMAL(6,2) NULL COMMENT '통합 건강도 (가중평균)',
  `risk_level` ENUM('SAFE','WARN','RISK') NOT NULL DEFAULT 'SAFE' COMMENT '종합 위험도',
  `calculation_logic_ver` INT NOT NULL DEFAULT 1 COMMENT '산출 로직 버전',
  `calculated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '계산 시각',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` TIMESTAMP NULL COMMENT '삭제일시 (Soft Delete)',
  `created_by` BIGINT NULL,
  `updated_by` BIGINT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ckm_company_quarter` (`company_id`, `quarter_id`),
  KEY `idx_ckm_quarter_risk_lookup` (`quarter_id`, `risk_level`, `composite_score` DESC),
  KEY `idx_ckm_company_latest` (`company_id`, `deleted_at`, `calculated_at` DESC),
  KEY `idx_ckm_version_trace` (`report_version_id`),
  CONSTRAINT `fk_ckm_company`
    FOREIGN KEY (`company_id`) REFERENCES `companies`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ckm_quarter`
    FOREIGN KEY (`quarter_id`) REFERENCES `quarters`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ckm_version`
    FOREIGN KEY (`report_version_id`) REFERENCES `company_report_versions`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='기업 분기별 핵심 건강도 및 위험등급';

CREATE TABLE `key_metric_descriptions` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `metric_code` VARCHAR(30) NOT NULL COMMENT '코드 (INTERNAL_HEALTH, EXTERNAL_HEALTH, COMPOSITE)',
  `metric_name` VARCHAR(100) NOT NULL COMMENT '표시명 (예: 내부 재무건강도)',
  `unit` VARCHAR(20) NULL COMMENT '표시 단위 (%, 점, 배 등)',
  `description` TEXT NOT NULL COMMENT '지표 정의 (무엇을 측정하는가)',
  `interpretation` TEXT NULL COMMENT '해석 방법 (점수별 구간 의미)',
  `action_hint` TEXT NULL COMMENT '투자/경영 액션 가이드',
  `weight` DECIMAL(3,2) NULL COMMENT '통합점수 산출 시 가중치 (0.00~1.00)',
  `threshold_safe` DECIMAL(6,2) NULL COMMENT 'SAFE 판정 임계값 (예: 70.00)',
  `threshold_warn` DECIMAL(6,2) NULL COMMENT 'WARN→RISK 경계값 (예: 40.00)',
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` TIMESTAMP NULL,
  `created_by` BIGINT NULL,
  `updated_by` BIGINT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_kmd_code` (`metric_code`),
  KEY `idx_kmd_active` (`is_active`, `deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='핵심 건강도 지표 메타 정의';
