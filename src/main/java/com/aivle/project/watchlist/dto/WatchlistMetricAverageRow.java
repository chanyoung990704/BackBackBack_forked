package com.aivle.project.watchlist.dto;

import java.math.BigDecimal;

public record WatchlistMetricAverageRow(
	String metricCode,
	String metricNameKo,
	BigDecimal avgValue,
	Long sampleCompanyCount
) {
}
