package com.aivle.project.metricaverage.service;

/**
 * 분기 단위 metric_averages 저장 결과.
 */
public record QuarterMetricAverageSaveResult(
	int insertedCount,
	int skippedCount
) {
}
