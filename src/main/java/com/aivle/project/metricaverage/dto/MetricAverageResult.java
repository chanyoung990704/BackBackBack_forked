package com.aivle.project.metricaverage.dto;

import java.math.BigDecimal;

/**
 * 지표 집계 결과 DTO.
 */
public record MetricAverageResult(
	Long metricId,
	BigDecimal avgValue,
	BigDecimal medianValue,
	BigDecimal minValue,
	BigDecimal maxValue,
	BigDecimal stddevValue,
	int companyCount
) {
}
