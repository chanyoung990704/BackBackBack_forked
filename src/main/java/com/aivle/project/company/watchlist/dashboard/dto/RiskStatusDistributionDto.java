package com.aivle.project.company.watchlist.dashboard.dto;

/**
 * 위험 상태 분포 DTO.
 */
public record RiskStatusDistributionDto(
	int NORMAL,
	int CAUTION,
	int RISK
) {
}
