-- SQL 쿼리 최적화를 위한 인덱스 추가

-- 1. 기업명 검색 성능 향상 (Full-Text Index)
-- 참고: MySQL 전용. H2에서는 무시되거나 별도 처리가 필요할 수 있음.
CREATE FULLTEXT INDEX idx_companies_corp_name_fts ON companies(corp_name);

-- 2. 보고서 지표 값 조회 성능 향상 (복합 인덱스)
-- 가장 빈번하게 조회되는 조건인 (버전, 값타입, 지표) 조합에 대한 인덱스
CREATE INDEX idx_crmv_lookup_v2 ON company_report_metric_values(report_version_id, value_type, metric_id);

-- 3. 워치리스트 대시보드 위험도 조회 성능 향상
-- RiskScoreSummaryEntity 조회 시 (기업, 분기) 조건 최적화
CREATE INDEX idx_rss_lookup ON risk_score_summaries(company_id, quarter_id);

-- 4. 게시글 목록 조회 성능 향상 (카테고리별 정렬 조회)
-- PostsEntity 조회 시 (카테고리, 삭제여부, 생성일시) 조합
-- 기존 idx_posts_category_created 인덱스가 있지만, status까지 포함한 조회 최적화
CREATE INDEX idx_posts_status_lookup ON posts(category_id, status, deleted_at, created_at DESC);
