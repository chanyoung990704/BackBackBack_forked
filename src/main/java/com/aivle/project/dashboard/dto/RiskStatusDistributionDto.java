package com.aivle.project.dashboard.dto;

/**
 * 위험 상태 분포 DTO.
 */
public record RiskStatusDistributionDto(
	int NORMAL,
	int CAUTION,
	int RISK
) {
}
