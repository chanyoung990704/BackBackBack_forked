package com.aivle.project.company.watchlist.dto;

import java.util.List;

public record WatchlistMetricAveragesResponse(
	int year,
	int quarter,
	List<WatchlistMetricAverageRow> metrics
) {
}
