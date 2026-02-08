-- SQL 쿼리 최적화를 위한 인덱스 추가 (H2 호환)

-- 1. 보고서 지표 값 조회 성능 향상
CREATE INDEX idx_crmv_lookup_v2 ON company_report_metric_values(report_version_id, value_type, metric_id);

-- 2. 워치리스트 대시보드 위험도 조회 성능 향상
CREATE INDEX idx_rss_lookup ON risk_score_summaries(company_id, quarter_id);

-- 3. 게시글 목록 조회 성능 향상
CREATE INDEX idx_posts_status_lookup ON posts(category_id, status, deleted_at, created_at DESC);
