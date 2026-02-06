package com.aivle.project.metricaverage.service;

import com.aivle.project.quarter.entity.QuartersEntity;
import com.aivle.project.quarter.repository.QuartersRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 저장된 전체 분기를 대상으로 지표 평균 집계를 배치 수행한다.
 */
@Service
@RequiredArgsConstructor
public class MetricAverageBatchService {

	private final QuartersRepository quartersRepository;
	private final MetricAverageCalculationService metricAverageCalculationService;

	@Transactional
	public int calculateAndUpsertAllQuarters() {
		List<QuartersEntity> quarters = quartersRepository.findAll();
		int processedQuarterCount = 0;
		for (QuartersEntity quarter : quarters) {
			metricAverageCalculationService.calculateAndUpsertByQuarter(quarter.getId());
			processedQuarterCount++;
		}
		return processedQuarterCount;
	}

	@Transactional
	public MetricAverageBatchSaveResult calculateAndInsertMissingAllQuarters(String triggerType, String executionId) {
		List<QuartersEntity> quarters = quartersRepository.findAll();
		int processedQuarterCount = 0;
		int insertedCount = 0;
		int skippedCount = 0;
		for (QuartersEntity quarter : quarters) {
			QuarterMetricAverageSaveResult result =
				metricAverageCalculationService.calculateAndInsertMissingByQuarter(quarter.getId());
			processedQuarterCount++;
			insertedCount += result.insertedCount();
			skippedCount += result.skippedCount();
		}
		return new MetricAverageBatchSaveResult(processedQuarterCount, insertedCount, skippedCount, triggerType, executionId);
	}
}
