package com.aivle.project.watchlist.dto;

import java.math.BigDecimal;

public record WatchlistMetricValueRow(
	Long watchlistId,
	Long companyId,
	String corpName,
	String corpCode,
	String metricCode,
	String metricNameKo,
	BigDecimal metricValue
) {
}
