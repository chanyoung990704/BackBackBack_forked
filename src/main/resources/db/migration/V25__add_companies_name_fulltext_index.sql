-- companies 기업 검색 성능 개선용 Fulltext 인덱스 (멱등)
SET @idx_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'companies'
      AND index_name = 'idx_companies_name_fts'
);

SET @create_sql := IF(
    @idx_exists = 0,
    'CREATE FULLTEXT INDEX idx_companies_name_fts ON companies(corp_name, corp_eng_name)',
    'SELECT 1'
);

PREPARE stmt FROM @create_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
