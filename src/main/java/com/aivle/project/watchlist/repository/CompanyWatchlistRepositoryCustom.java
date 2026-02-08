package com.aivle.project.watchlist.repository;

import com.aivle.project.metric.entity.MetricValueType;
import com.aivle.project.risk.entity.RiskLevel;
import com.aivle.project.watchlist.dto.WatchlistDashboardMetricRow;
import com.aivle.project.watchlist.dto.WatchlistDashboardRiskRow;
import com.aivle.project.watchlist.dto.WatchlistMetricAverageRow;
import com.aivle.project.watchlist.dto.WatchlistMetricValueRow;
import java.util.Collection;
import java.util.List;

public interface CompanyWatchlistRepositoryCustom {

	List<WatchlistDashboardMetricRow> findDashboardMetrics(
		Long userId,
		short year,
		byte quarter,
		MetricValueType valueType,
		Collection<String> metricCodes,
		boolean metricCodesEmpty
	);

	List<WatchlistDashboardRiskRow> findDashboardRisks(
		Long userId,
		Long quarterId,
		RiskLevel riskLevel
	);

	/**
	 * Service에서의 그룹화를 위해 year, quarter를 포함한 Projection 인터페이스를 유지하거나
	 * 전용 DTO를 사용해야 합니다. 여기서는 호환성을 위해 Projection 인터페이스를 반환합니다.
	 */
	List<WatchlistMetricValueProjection> findWatchlistMetricValues(
		Long userId,
		short year,
		byte quarter,
		MetricValueType valueType
	);

	List<WatchlistMetricValueProjection> findWatchlistMetricValuesInRange(
		Long userId,
		short fromYear,
		byte fromQuarter,
		short toYear,
		byte toQuarter,
		MetricValueType valueType
	);

	List<WatchlistMetricAverageRow> findWatchlistMetricAverages(
		Long userId,
		short year,
		byte quarter,
		MetricValueType valueType,
		Collection<String> metricCodes,
		boolean metricCodesEmpty
	);
}