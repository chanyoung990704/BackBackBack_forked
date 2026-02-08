-- 새로운 위험 지표 추가 (업종 상대 지표)
INSERT INTO metrics (metric_code, metric_name_ko, metric_name_en, is_risk_indicator, created_at, updated_at)
VALUES 
('ROA_IndRel', 'ROA 업종상대', 'ROAIndustryRelative', 1, NOW(), NOW()),
('DbRatio_IndRel', '부채비율 업종상대', 'DebtRatioIndustryRelative', 1, NOW(), NOW()),
('STDebtRatio_IndRel', '단기차입금비율 업종상대', 'ShortTermDebtRatioIndustryRelative', 1, NOW(), NOW()),
('CurRatio_IndRel', '유동비율 업종상대', 'CurrentRatioIndustryRelative', 1, NOW(), NOW()),
('CFO_AsRatio_IndRel', 'CFO 자산비율 업종상대', 'CFOToAssetsRatioIndustryRelative', 1, NOW(), NOW());