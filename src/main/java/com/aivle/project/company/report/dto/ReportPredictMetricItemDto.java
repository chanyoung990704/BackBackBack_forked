package com.aivle.project.company.report.dto;

import java.math.BigDecimal;

/**
 * 예측 지표 항목 DTO.
 */
public record ReportPredictMetricItemDto(
	String metricCode,
	String metricNameKo,
	BigDecimal metricValue,
	int quarterKey
) {
}
