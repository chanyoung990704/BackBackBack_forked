-- 24. Key metric descriptions seed data (H2)
MERGE INTO key_metric_descriptions (
  metric_code,
  metric_name,
  unit,
  description,
  interpretation,
  action_hint,
  weight,
  threshold_safe,
  threshold_warn,
  is_active,
  created_at,
  updated_at
) KEY (metric_code)
VALUES
(
  'NETWORK_HEALTH',
  '내부 건강도',
  '%',
  '연결된 협력사까지 포함한 네트워크 안정성 점수입니다.',
  '높을수록 리스크 전파 가능성이 낮습니다.',
  '하락 시 연관 협력사 신호등을 먼저 확인하세요.',
  0.60,
  70.00,
  40.00,
  TRUE,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
),
(
  'EXTERNAL_HEALTH',
  '외부 건강도',
  '%',
  '외부 평판·시장 신호를 종합한 건강도 지표입니다.',
  '높을수록 대외 리스크 신호가 안정적입니다.',
  '하락 시 외부 이슈 모니터링과 커뮤니케이션을 점검하세요.',
  0.40,
  70.00,
  40.00,
  TRUE,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
),
(
  'EXTERNAL_REPUTATION',
  '외부 기업 평판',
  '점',
  '외부 뉴스·커뮤니티·공시 텍스트 등 비정형 데이터를 분석해 평판 점수를 산출합니다.',
  '최근 30일 언급량과 감성을 종합한 지표입니다.',
  '부정 키워드 급증 시 커뮤니케이션 전략을 점검하세요.',
  NULL,
  70.00,
  40.00,
  TRUE,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
);
