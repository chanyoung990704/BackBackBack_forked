package com.aivle.project.risk.service;

import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.repository.CompaniesRepository;
import com.aivle.project.metric.entity.MetricValueType;
import com.aivle.project.quarter.entity.QuartersEntity;
import com.aivle.project.quarter.repository.QuartersRepository;
import com.aivle.project.report.entity.CompanyReportVersionsEntity;
import com.aivle.project.report.repository.CompanyReportMetricValuesRepository;
import com.aivle.project.report.repository.CompanyReportVersionsRepository;
import com.aivle.project.risk.dto.RiskAggregationResult;
import com.aivle.project.risk.entity.RiskLevel;
import com.aivle.project.risk.entity.RiskScoreSummaryEntity;
import com.aivle.project.risk.repository.RiskScoreSummaryRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 위험 지표 평균을 기반으로 기업-분기-버전 위험도를 계산한다.
 */
@Service
@RequiredArgsConstructor
public class RiskScoreCalculationService {

	private final CompanyReportMetricValuesRepository metricValuesRepository;
	private final RiskScoreSummaryRepository riskScoreSummaryRepository;
	private final CompaniesRepository companiesRepository;
	private final QuartersRepository quartersRepository;
	private final CompanyReportVersionsRepository companyReportVersionsRepository;

	@Value("${risk.threshold.caution:40}")
	private BigDecimal cautionThreshold;

	@Value("${risk.threshold.danger:70}")
	private BigDecimal dangerThreshold;

	@Transactional
	public RiskAggregationResult calculateAndUpsert(Long companyId, Long quarterId, Long reportVersionId) {
		List<BigDecimal> values = metricValuesRepository.findRiskMetricValuesByCompanyQuarterAndVersion(
			companyId,
			quarterId,
			reportVersionId,
			MetricValueType.ACTUAL
		);

		int count = values.size();
		BigDecimal average = calculateAverage(values);
		BigDecimal score = average;
		RiskLevel level = evaluateRiskLevel(average);
		LocalDateTime now = LocalDateTime.now();

		RiskScoreSummaryEntity summary = riskScoreSummaryRepository
			.findByCompanyIdAndQuarterIdAndReportVersionId(companyId, quarterId, reportVersionId)
			.orElseGet(() -> createSummarySkeleton(companyId, quarterId, reportVersionId, now));

		summary.refresh(score, level, count, average, now);
		riskScoreSummaryRepository.save(summary);

		return new RiskAggregationResult(companyId, quarterId, reportVersionId, score, level, count, average);
	}

	RiskLevel evaluateRiskLevel(BigDecimal avgRiskValue) {
		if (avgRiskValue == null) {
			return RiskLevel.UNDEFINED;
		}
		if (avgRiskValue.compareTo(dangerThreshold) >= 0) {
			return RiskLevel.DANGER;
		}
		if (avgRiskValue.compareTo(cautionThreshold) >= 0) {
			return RiskLevel.CAUTION;
		}
		return RiskLevel.SAFE;
	}

	private BigDecimal calculateAverage(List<BigDecimal> values) {
		if (values.isEmpty()) {
			return null;
		}
		BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
		return sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
	}

	private RiskScoreSummaryEntity createSummarySkeleton(Long companyId, Long quarterId, Long reportVersionId, LocalDateTime now) {
		CompaniesEntity company = companiesRepository.getReferenceById(companyId);
		QuartersEntity quarter = quartersRepository.getReferenceById(quarterId);
		CompanyReportVersionsEntity reportVersion = companyReportVersionsRepository.getReferenceById(reportVersionId);
		return RiskScoreSummaryEntity.create(company, quarter, reportVersion, null, RiskLevel.UNDEFINED, 0, null, now);
	}
}
