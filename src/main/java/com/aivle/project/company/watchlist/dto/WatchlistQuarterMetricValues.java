package com.aivle.project.company.watchlist.dto;

import java.util.List;

public record WatchlistQuarterMetricValues(
	int year,
	int quarter,
	List<WatchlistMetricValueRow> items
) {
}
