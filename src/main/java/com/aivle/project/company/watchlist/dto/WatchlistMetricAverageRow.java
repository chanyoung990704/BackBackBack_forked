package com.aivle.project.company.watchlist.dto;

import java.math.BigDecimal;

public record WatchlistMetricAverageRow(
	String metricCode,
	String metricNameKo,
	BigDecimal avgValue,
	Long sampleCompanyCount
) {
}
