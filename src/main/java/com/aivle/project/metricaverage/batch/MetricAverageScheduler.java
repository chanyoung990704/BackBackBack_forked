package com.aivle.project.metricaverage.batch;

import com.aivle.project.metricaverage.service.MetricAverageBatchSaveResult;
import com.aivle.project.metricaverage.service.MetricAverageBatchService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * metric_averages 일일 집계 스케줄러.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "metric-average.schedule", name = "enabled", havingValue = "true")
public class MetricAverageScheduler {

	private final MetricAverageBatchService metricAverageBatchService;

	@Scheduled(cron = "${metric-average.schedule.cron:0 0 3 * * *}")
	public void saveMissingMetricAveragesDaily() {
		MetricAverageBatchSaveResult result = metricAverageBatchService.calculateAndInsertMissingAllQuarters(
			"SCHEDULE",
			UUID.randomUUID().toString()
		);
		log.info("metric_averages 스케줄 저장 완료. triggerType={}, executionId={}, processedQuarterCount={}, insertedCount={}, skippedCount={}",
			result.triggerType(), result.executionId(), result.processedQuarterCount(), result.insertedCount(), result.skippedCount());
	}
}
