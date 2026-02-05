package com.aivle.project.metricaverage.service;

import com.aivle.project.metric.entity.MetricValueType;
import com.aivle.project.metric.entity.MetricsEntity;
import com.aivle.project.metric.repository.MetricsRepository;
import com.aivle.project.metricaverage.dto.MetricAverageResult;
import com.aivle.project.metricaverage.entity.MetricAverageEntity;
import com.aivle.project.metricaverage.repository.MetricAverageRepository;
import com.aivle.project.quarter.entity.QuartersEntity;
import com.aivle.project.quarter.repository.QuartersRepository;
import com.aivle.project.report.dto.MetricValueSampleProjection;
import com.aivle.project.report.repository.CompanyReportMetricValuesRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 분기별 비위험 지표 통계를 계산한다.
 */
@Service
@RequiredArgsConstructor
public class MetricAverageCalculationService {

	private static final int DATA_SOURCE_VERSION = 1;
	private static final int SCALE = 4;

	private final CompanyReportMetricValuesRepository metricValuesRepository;
	private final MetricAverageRepository metricAverageRepository;
	private final QuartersRepository quartersRepository;
	private final MetricsRepository metricsRepository;

	@Transactional
	public List<MetricAverageResult> calculateAndUpsertByQuarter(Long quarterId) {
		List<MetricValueSampleProjection> samples = metricValuesRepository.findNonRiskActualMetricSamplesByQuarterId(
			quarterId,
			MetricValueType.ACTUAL
		);

		Map<Long, List<BigDecimal>> valuesByMetric = new HashMap<>();
		for (MetricValueSampleProjection sample : samples) {
			valuesByMetric.computeIfAbsent(sample.getMetricId(), ignored -> new ArrayList<>()).add(sample.getMetricValue());
		}

		List<MetricAverageResult> results = new ArrayList<>();
		LocalDateTime now = LocalDateTime.now();
		for (Map.Entry<Long, List<BigDecimal>> entry : valuesByMetric.entrySet()) {
			Long metricId = entry.getKey();
			List<BigDecimal> values = entry.getValue();
			MetricAverageResult result = calculate(metricId, values);
			upsert(quarterId, result, now);
			results.add(result);
		}
		return results;
	}

	MetricAverageResult calculate(Long metricId, List<BigDecimal> values) {
		if (values == null || values.isEmpty()) {
			return new MetricAverageResult(metricId, null, null, null, null, null, 0);
		}

		List<BigDecimal> sorted = values.stream().sorted().toList();
		int count = sorted.size();
		BigDecimal sum = sorted.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal avg = scale(sum.divide(BigDecimal.valueOf(count), SCALE, RoundingMode.HALF_UP));
		BigDecimal median = calculateMedian(sorted);
		BigDecimal min = scale(sorted.get(0));
		BigDecimal max = scale(sorted.get(count - 1));
		BigDecimal stddev = calculateStddev(sorted, avg);
		return new MetricAverageResult(metricId, avg, median, min, max, stddev, count);
	}

	private void upsert(Long quarterId, MetricAverageResult result, LocalDateTime now) {
		MetricAverageEntity entity = metricAverageRepository.findByQuarterIdAndMetricId(quarterId, result.metricId())
			.orElseGet(() -> createSkeleton(quarterId, result.metricId(), now));
		entity.refresh(
			result.avgValue(),
			result.medianValue(),
			result.minValue(),
			result.maxValue(),
			result.stddevValue(),
			result.companyCount(),
			now,
			DATA_SOURCE_VERSION
		);
		metricAverageRepository.save(entity);
	}

	private MetricAverageEntity createSkeleton(Long quarterId, Long metricId, LocalDateTime now) {
		QuartersEntity quarter = quartersRepository.getReferenceById(quarterId);
		MetricsEntity metric = metricsRepository.getReferenceById(metricId);
		return MetricAverageEntity.create(quarter, metric, null, null, null, null, null, 0, now, DATA_SOURCE_VERSION);
	}

	private BigDecimal calculateMedian(List<BigDecimal> sorted) {
		int size = sorted.size();
		if (size % 2 == 1) {
			return scale(sorted.get(size / 2));
		}
		BigDecimal left = sorted.get(size / 2 - 1);
		BigDecimal right = sorted.get(size / 2);
		return scale(left.add(right).divide(BigDecimal.valueOf(2), SCALE, RoundingMode.HALF_UP));
	}

	private BigDecimal calculateStddev(List<BigDecimal> sorted, BigDecimal avg) {
		if (sorted.size() <= 1) {
			return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
		}
		BigDecimal sumSquared = BigDecimal.ZERO;
		for (BigDecimal value : sorted) {
			BigDecimal diff = value.subtract(avg);
			sumSquared = sumSquared.add(diff.multiply(diff));
		}
		BigDecimal variance = sumSquared.divide(BigDecimal.valueOf(sorted.size()), SCALE + 6, RoundingMode.HALF_UP);
		double stddev = Math.sqrt(variance.doubleValue());
		return scale(BigDecimal.valueOf(stddev));
	}

	private BigDecimal scale(BigDecimal value) {
		return value.setScale(SCALE, RoundingMode.HALF_UP);
	}
}
