package com.aivle.project.company.risk.dto;

import com.aivle.project.company.risk.entity.RiskLevel;
import java.math.BigDecimal;

/**
 * 위험도 계산 결과 DTO.
 */
public record RiskAggregationResult(
	Long companyId,
	Long quarterId,
	Long reportVersionId,
	BigDecimal riskScore,
	RiskLevel riskLevel,
	int riskMetricsCount,
	BigDecimal riskMetricsAvg
) {
}
