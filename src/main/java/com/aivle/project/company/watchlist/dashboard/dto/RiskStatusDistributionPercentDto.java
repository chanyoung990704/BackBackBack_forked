package com.aivle.project.company.watchlist.dashboard.dto;

/**
 * 위험 상태 분포 비율 DTO.
 */
public record RiskStatusDistributionPercentDto(
	double NORMAL,
	double CAUTION,
	double RISK
) {
}
