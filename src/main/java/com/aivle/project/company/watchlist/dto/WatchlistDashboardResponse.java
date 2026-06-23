package com.aivle.project.company.watchlist.dto;

import java.util.List;

public record WatchlistDashboardResponse(
	int year,
	int quarter,
	List<WatchlistDashboardMetricRow> metrics,
	List<WatchlistDashboardRiskRow> risks
) {
}
