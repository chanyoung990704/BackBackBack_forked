package com.aivle.project.company.watchlist.repository;

import com.aivle.project.company.risk.entity.RiskLevel;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface WatchlistDashboardRiskProjection {
	Long getWatchlistId();
	Long getCompanyId();
	String getCorpName();
	BigDecimal getRiskScore();
	RiskLevel getRiskLevel();
	BigDecimal getRiskMetricsAvg();
	LocalDateTime getLastRefreshedAt();
}
