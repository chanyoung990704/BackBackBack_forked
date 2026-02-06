SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'companies'
      AND COLUMN_NAME = 'industry_code_id'
);
SET @column_sql := IF(
        @column_exists = 0,
        'ALTER TABLE `companies` ADD COLUMN `industry_code_id` BIGINT NULL COMMENT ''산업 코드 ID'' AFTER `stock_code`',
        'SELECT 1'
                  );
PREPARE stmt FROM @column_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'companies'
      AND INDEX_NAME = 'idx_companies_industry_code'
);
SET @index_sql := IF(
        @index_exists = 0,
        'ALTER TABLE `companies` ADD INDEX `idx_companies_industry_code` (`industry_code_id`)',
        'SELECT 1'
                 );
PREPARE stmt FROM @index_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @fk_exists := (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'companies'
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
      AND CONSTRAINT_NAME = 'fk_companies_industry_code'
);
SET @fk_sql := IF(
        @fk_exists = 0,
        'ALTER TABLE `companies` ADD CONSTRAINT `fk_companies_industry_code` FOREIGN KEY (`industry_code_id`) REFERENCES `industry_codes`(`id`) ON DELETE SET NULL',
        'SELECT 1'
              );
PREPARE stmt FROM @fk_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
