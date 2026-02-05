package com.aivle.project.watchlist.dto;

import com.aivle.project.risk.entity.RiskLevel;
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
