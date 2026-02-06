package com.aivle.project.watchlist.dto;

import java.util.List;

public record WatchlistMetricValuesResponse(
	List<WatchlistQuarterMetricValues> quarters
) {
}
