package com.aivle.project.dashboard.dto;

/**
 * 대시보드 리스크 체류 계산용 기업-분기 위험 상태 DTO.
 */
public record CompanyQuarterRiskDto(
	String companyId,
	String companyName,
	String quarter,
	RiskLevel riskLevel
) {
	public enum RiskLevel {
		MIN,
		WARN,
		RISK
	}
}
