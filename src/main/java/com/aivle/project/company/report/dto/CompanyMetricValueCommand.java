package com.aivle.project.company.report.dto;

import java.math.BigDecimal;

/**
 * 기업별 지표 값 적재 명령.
 */
public record CompanyMetricValueCommand(
	String stockCode,
	String metricCode,
	int quarterOffset,
	BigDecimal metricValue,
	int rowIndex,
	int colIndex,
	String headerName
) {
}
