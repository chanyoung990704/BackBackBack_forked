ALTER TABLE metrics
ADD COLUMN is_risk_indicator TINYINT(1) NOT NULL DEFAULT 0 COMMENT '리스크 지표 여부' AFTER unit;
