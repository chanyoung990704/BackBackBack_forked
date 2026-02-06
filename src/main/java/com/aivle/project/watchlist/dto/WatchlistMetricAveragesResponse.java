package com.aivle.project.watchlist.dto;

import java.util.List;

public record WatchlistMetricAveragesResponse(
	int year,
	int quarter,
	List<WatchlistMetricAverageRow> metrics
) {
}
