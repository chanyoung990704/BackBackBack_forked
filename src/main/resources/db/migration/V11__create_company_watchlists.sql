CREATE TABLE `company_watchlists` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '사용자 ID',
  `company_id` BIGINT NOT NULL COMMENT '기업 ID',
  `note` VARCHAR(255) NULL COMMENT '메모',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` TIMESTAMP NULL COMMENT '삭제일시 (soft delete)',
  `created_by` BIGINT NULL,
  `updated_by` BIGINT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_company` (`user_id`, `company_id`),
  INDEX `idx_cw_user` (`user_id`),
  INDEX `idx_cw_company` (`company_id`),
  INDEX `idx_cw_user_deleted` (`user_id`, `deleted_at`),
  CONSTRAINT `fk_cw_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_cw_company` FOREIGN KEY (`company_id`) REFERENCES `companies`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='사용자 기업 위시리스트';
