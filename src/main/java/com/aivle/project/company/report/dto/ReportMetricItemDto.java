package com.aivle.project.company.report.dto;

import com.aivle.project.company.metric.entity.MetricValueType;
import java.math.BigDecimal;

/**
 * 분기별 지표 항목 DTO.
 */
public record ReportMetricItemDto(
	String metricCode,
	String metricNameKo,
	BigDecimal metricValue,
	MetricValueType valueType
) {
}
