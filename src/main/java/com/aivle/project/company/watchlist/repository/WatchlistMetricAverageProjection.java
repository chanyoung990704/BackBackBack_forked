package com.aivle.project.company.watchlist.repository;

import java.math.BigDecimal;

public interface WatchlistMetricAverageProjection {
	String getMetricCode();
	String getMetricNameKo();
	BigDecimal getAvgValue();
	Long getSampleCompanyCount();
}
