package com.aivle.project.company.service;

import com.aivle.project.company.client.AiServerClient;
import com.aivle.project.company.dto.AiAnalysisResponse;
import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.repository.CompaniesRepository;
import com.aivle.project.common.util.GetOrCreateResolver;
import com.aivle.project.metric.entity.MetricValueType;
import com.aivle.project.metric.entity.MetricsEntity;
import com.aivle.project.metric.repository.MetricsRepository;
import com.aivle.project.quarter.entity.QuartersEntity;
import com.aivle.project.quarter.repository.QuartersRepository;
import com.aivle.project.quarter.support.QuarterCalculator;
import com.aivle.project.quarter.support.YearQuarter;
import com.aivle.project.report.entity.CompanyReportMetricValuesEntity;
import com.aivle.project.report.entity.CompanyReportVersionsEntity;
import com.aivle.project.report.entity.CompanyReportsEntity;
import com.aivle.project.report.repository.CompanyReportMetricValuesRepository;
import com.aivle.project.report.repository.CompanyReportVersionsRepository;
import com.aivle.project.report.repository.CompanyReportsRepository;
import com.aivle.project.report.service.CompanyReportVersionIssueService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기업 개요 조회용 예측값 캐시 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyPredictionCacheService {

	private final AiServerClient aiServerClient;
	private final CompaniesRepository companiesRepository;
	private final QuartersRepository quartersRepository;
	private final CompanyReportsRepository companyReportsRepository;
	private final CompanyReportVersionsRepository companyReportVersionsRepository;
	private final MetricsRepository metricsRepository;
	private final CompanyReportMetricValuesRepository companyReportMetricValuesRepository;
	private final CompanyReportVersionIssueService companyReportVersionIssueService;

	/**
	 * 최신 ACTUAL 분기 기준으로 다음 분기 예측값을 캐시한다.
	 * 요청 분기가 최신 ACTUAL 분기와 다르면 AI 호출을 하지 않는다.
	 */
	@Transactional
	public void ensurePredictionCached(Long companyId, int requestedQuarterKey) {
		CompaniesEntity company = companiesRepository.findById(companyId)
			.orElseThrow(() -> new IllegalArgumentException("Company not found: " + companyId));

		Optional<Integer> latestActualQuarterKey = companyReportMetricValuesRepository
			.findMaxActualQuarterKeyByStockCode(company.getStockCode());
		if (latestActualQuarterKey.isEmpty()) {
			log.info("No actual quarter found for company: {}", companyId);
			return;
		}

		int latestActualKey = latestActualQuarterKey.get();
		if (requestedQuarterKey != latestActualKey) {
			log.info("Skip prediction cache. requestedQuarterKey={}, latestActualKey={}", requestedQuarterKey, latestActualKey);
			return;
		}

		YearQuarter baseQuarter = QuarterCalculator.parseQuarterKey(latestActualKey);
		YearQuarter targetQuarter = QuarterCalculator.offset(baseQuarter, 1);
		QuartersEntity targetQuarterEntity = getOrCreateQuarter(targetQuarter);

		CompanyReportsEntity report = getOrCreateReport(company, targetQuarterEntity);

		CompanyReportVersionsEntity latestVersion = companyReportVersionsRepository
			.findTopByCompanyReportOrderByVersionNoDesc(report)
			.orElseGet(() -> companyReportVersionIssueService.issueNextVersion(report, true, null));

		if (companyReportMetricValuesRepository.existsByReportVersionAndValueTypeAndMetricValueIsNotNull(
			latestVersion,
			MetricValueType.PREDICTED
		)) {
			log.info("Prediction cache already exists for reportVersionId={}", latestVersion.getId());
			return;
		}

		AiAnalysisResponse response = aiServerClient.getPrediction(company.getStockCode());
		if (response == null || response.predictions() == null || response.basePeriod() == null) {
			log.warn("Empty AI prediction response for company: {}", company.getStockCode());
			return;
		}

		int basePeriod = parseBasePeriod(response.basePeriod());
		if (basePeriod != latestActualKey) {
			log.warn("AI basePeriod mismatch. expected={}, actual={}", latestActualKey, basePeriod);
			return;
		}

		savePredictions(latestVersion, targetQuarterEntity, response.predictions());
	}

	private QuartersEntity getOrCreateQuarter(YearQuarter quarter) {
		return GetOrCreateResolver.resolve(
			() -> quartersRepository.findByYearAndQuarter((short) quarter.year(), (byte) quarter.quarter()),
			() -> quartersRepository.save(QuartersEntity.create(
				quarter.year(),
				quarter.quarter(),
				quarter.toQuarterKey(),
				QuarterCalculator.startDate(quarter),
				QuarterCalculator.endDate(quarter)
			)),
			() -> quartersRepository.findByYearAndQuarter((short) quarter.year(), (byte) quarter.quarter())
		);
	}

	private CompanyReportsEntity getOrCreateReport(CompaniesEntity company, QuartersEntity quarter) {
		return GetOrCreateResolver.resolve(
			() -> companyReportsRepository.findByCompanyIdAndQuarterId(company.getId(), quarter.getId()),
			() -> companyReportsRepository.save(CompanyReportsEntity.create(company, quarter, null)),
			() -> companyReportsRepository.findByCompanyIdAndQuarterId(company.getId(), quarter.getId())
		);
	}

	private void savePredictions(
		CompanyReportVersionsEntity reportVersion,
		QuartersEntity quarter,
		Map<String, Double> predictions
	) {
		List<MetricsEntity> metrics = metricsRepository.findAllByMetricCodeIn(predictions.keySet());
		Map<String, MetricsEntity> metricMap = metrics.stream()
			.collect(Collectors.toMap(MetricsEntity::getMetricCode, Function.identity()));

		for (Map.Entry<String, Double> entry : predictions.entrySet()) {
			MetricsEntity metric = metricMap.get(entry.getKey());
			Double value = entry.getValue();
			if (metric == null || value == null) {
				continue;
			}
			CompanyReportMetricValuesEntity metricValue = CompanyReportMetricValuesEntity.create(
				reportVersion,
				metric,
				quarter,
				BigDecimal.valueOf(value),
				MetricValueType.PREDICTED
			);
			companyReportMetricValuesRepository.save(metricValue);
		}
	}

	private int parseBasePeriod(String basePeriod) {
		try {
			int parsed = Integer.parseInt(basePeriod);
			YearQuarter yearQuarter = QuarterCalculator.parseQuarterKey(parsed);
			return yearQuarter.toQuarterKey();
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("base_period는 숫자 형식이어야 합니다.", e);
		}
	}
}
