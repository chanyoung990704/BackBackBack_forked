package com.aivle.project.dashboard.dto;

import java.util.List;

/**
 * 대시보드 요약 응답 DTO.
 */
public record DashboardSummaryResponse(
	String range,
	List<KpiCardDto> kpis,
	String latestActualQuarter,
	String forecastQuarter,
	List<String> windowQuarters,
	RiskStatusDistributionDto riskStatusDistribution,
	RiskStatusDistributionPercentDto riskStatusDistributionPercent,
	Double averageRiskLevel,
	MajorSectorDto majorSector,
	List<RiskStatusBucketDto> riskStatusDistributionTrend
) {
}
