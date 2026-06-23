package com.aivle.project.company.metric.metricaverage.service;

/**
 * 전체 분기 metric_averages 저장 결과.
 */
public record MetricAverageBatchSaveResult(
	int processedQuarterCount,
	int insertedCount,
	int skippedCount,
	String triggerType,
	String executionId
) {
}
