package com.aivle.project.company.watchlist.dto;

import com.aivle.project.company.risk.entity.RiskLevel;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WatchlistDashboardRiskRow(
	Long watchlistId,
	Long companyId,
	String corpName,
	BigDecimal riskScore,
	RiskLevel riskLevel,
	BigDecimal riskMetricsAvg,
	LocalDateTime lastRefreshedAt
) {
}
