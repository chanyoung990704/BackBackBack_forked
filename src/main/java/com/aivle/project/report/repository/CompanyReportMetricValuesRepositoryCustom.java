package com.aivle.project.report.repository;

import com.aivle.project.metric.entity.MetricValueType;
import com.aivle.project.report.dto.MetricValueSampleProjection;
import com.aivle.project.report.dto.ReportMetricRowProjection;
import com.aivle.project.report.dto.ReportPredictMetricRowProjection;
import java.math.BigDecimal;
import java.util.List;

public interface CompanyReportMetricValuesRepositoryCustom {

	List<ReportMetricRowProjection> findLatestMetricsByStockCodeAndQuarterRange(
		String stockCode,
		int fromQuarterKey,
		int toQuarterKey
	);

	List<ReportMetricRowProjection> findLatestMetricsByStockCodeAndQuarterRangeAndMetricCodes(
		String stockCode,
		int fromQuarterKey,
		int toQuarterKey,
		List<String> metricCodes
	);

	List<ReportPredictMetricRowProjection> findLatestMetricsByStockCodeAndQuarterKeyAndType(
		String stockCode,
		int quarterKey,
		MetricValueType valueType
	);

	List<com.aivle.project.report.dto.CompanyOverviewMetricRowProjection> findLatestOverviewMetricsByCompanyQuarter(
		Long companyId,
		Long quarterId,
		MetricValueType valueType,
		String locale
	);

	List<com.aivle.project.report.dto.CompanyOverviewMetricRowProjection> findLatestOverviewMetricsByStockCodeAndQuarterRange(
		String stockCode,
		int fromQuarterKey,
		int toQuarterKey,
		String locale
	);

	List<BigDecimal> findRiskMetricValuesByCompanyQuarterAndVersion(
		Long companyId,
		Long quarterId,
		Long reportVersionId,
		MetricValueType valueType
	);

	List<MetricValueSampleProjection> findNonRiskActualMetricSamplesByQuarterId(
		Long quarterId,
		MetricValueType valueType
	);
}
