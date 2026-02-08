package com.aivle.project.risk.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.aivle.project.common.config.QuerydslConfig;
import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.repository.CompaniesRepository;
import com.aivle.project.metric.entity.MetricValueType;
import com.aivle.project.metric.entity.MetricsEntity;
import com.aivle.project.metric.repository.MetricsRepository;
import com.aivle.project.quarter.entity.QuartersEntity;
import com.aivle.project.quarter.repository.QuartersRepository;
import com.aivle.project.report.entity.CompanyReportMetricValuesEntity;
import com.aivle.project.report.entity.CompanyReportVersionsEntity;
import com.aivle.project.report.entity.CompanyReportsEntity;
import com.aivle.project.report.repository.CompanyReportMetricValuesRepository;
import com.aivle.project.report.repository.CompanyReportVersionsRepository;
import com.aivle.project.report.repository.CompanyReportsRepository;
import com.aivle.project.risk.dto.RiskAggregationResult;
import com.aivle.project.risk.entity.RiskLevel;
import com.aivle.project.risk.entity.RiskScoreSummaryEntity;
import com.aivle.project.risk.repository.RiskScoreSummaryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import({QuerydslConfig.class, RiskScoreCalculationService.class})
class RiskScoreCalculationServiceTest {

	@Autowired
	private RiskScoreCalculationService riskScoreCalculationService;

	@Autowired
	private CompaniesRepository companiesRepository;

	@Autowired
	private QuartersRepository quartersRepository;

	@Autowired
	private CompanyReportsRepository companyReportsRepository;

	@Autowired
	private CompanyReportVersionsRepository companyReportVersionsRepository;

	@Autowired
	private CompanyReportMetricValuesRepository companyReportMetricValuesRepository;

	@Autowired
	private MetricsRepository metricsRepository;

	@Autowired
	private RiskScoreSummaryRepository riskScoreSummaryRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("리스크 지표 ACTUAL 값 평균으로 위험도를 계산한다")
	void calculateWithRiskMetricsOnly() {
		// given
		CompaniesEntity company = companiesRepository.save(CompaniesEntity.create(
			"00000011", "테스트기업", "TEST", "111111", LocalDate.of(2025, 1, 1)
		));
		QuartersEntity quarter = quartersRepository.save(QuartersEntity.create(
			2025, 3, 20253, LocalDate.of(2025, 7, 1), LocalDate.of(2025, 9, 30)
		));
		CompanyReportsEntity report = companyReportsRepository.save(CompanyReportsEntity.create(company, quarter, null));
		CompanyReportVersionsEntity version = companyReportVersionsRepository.save(
			CompanyReportVersionsEntity.create(report, 1, LocalDateTime.now(), false, null)
		);

		MetricsEntity roa = metricsRepository.findByMetricCode("ROA").orElseThrow();
		MetricsEntity opMargin = metricsRepository.findByMetricCode("OpMargin").orElseThrow();
		MetricsEntity roe = metricsRepository.findByMetricCode("ROE").orElseThrow();
		jdbcTemplate.update("UPDATE metrics SET is_risk_indicator = 1 WHERE id IN (?, ?)", roa.getId(), opMargin.getId());

		companyReportMetricValuesRepository.save(CompanyReportMetricValuesEntity.create(
			version, roa, quarter, new BigDecimal("80"), MetricValueType.ACTUAL
		));
		companyReportMetricValuesRepository.save(CompanyReportMetricValuesEntity.create(
			version, opMargin, quarter, new BigDecimal("60"), MetricValueType.ACTUAL
		));
		companyReportMetricValuesRepository.save(CompanyReportMetricValuesEntity.create(
			version, roe, quarter, new BigDecimal("99"), MetricValueType.ACTUAL
		));
		companyReportMetricValuesRepository.save(CompanyReportMetricValuesEntity.create(
			version, roa, quarter, new BigDecimal("20"), MetricValueType.PREDICTED
		));

		// when
		RiskAggregationResult result = riskScoreCalculationService.calculateAndUpsert(
			company.getId(), quarter.getId(), version.getId()
		);

		// then
		assertThat(result.riskMetricsCount()).isEqualTo(2);
		assertThat(result.riskMetricsAvg()).isEqualByComparingTo("70.00");
		assertThat(result.riskScore()).isEqualByComparingTo("70.00");
		assertThat(result.riskLevel()).isEqualTo(RiskLevel.DANGER);

		RiskScoreSummaryEntity saved = riskScoreSummaryRepository
			.findByCompanyIdAndQuarterIdAndReportVersionId(company.getId(), quarter.getId(), version.getId())
			.orElseThrow();
		assertThat(saved.getRiskMetricsCount()).isEqualTo(2);
		assertThat(saved.getRiskMetricsAvg()).isEqualByComparingTo("70.00");
		assertThat(saved.getRiskLevel()).isEqualTo(RiskLevel.DANGER);
	}

	@Test
	@DisplayName("집계 대상 리스크 ACTUAL 값이 없으면 UNDEFINED로 저장한다")
	void calculateUndefinedWhenNoRiskActualValues() {
		// given
		CompaniesEntity company = companiesRepository.save(CompaniesEntity.create(
			"00000012", "테스트기업2", "TEST2", "222222", LocalDate.of(2025, 1, 1)
		));
		QuartersEntity quarter = quartersRepository.save(QuartersEntity.create(
			2025, 3, 20253, LocalDate.of(2025, 7, 1), LocalDate.of(2025, 9, 30)
		));
		CompanyReportsEntity report = companyReportsRepository.save(CompanyReportsEntity.create(company, quarter, null));
		CompanyReportVersionsEntity version = companyReportVersionsRepository.save(
			CompanyReportVersionsEntity.create(report, 1, LocalDateTime.now(), false, null)
		);

		MetricsEntity roe = metricsRepository.findByMetricCode("ROE").orElseThrow();
		companyReportMetricValuesRepository.save(CompanyReportMetricValuesEntity.create(
			version, roe, quarter, new BigDecimal("50"), MetricValueType.ACTUAL
		));

		// when
		RiskAggregationResult result = riskScoreCalculationService.calculateAndUpsert(
			company.getId(), quarter.getId(), version.getId()
		);

		// then
		assertThat(result.riskMetricsCount()).isZero();
		assertThat(result.riskMetricsAvg()).isNull();
		assertThat(result.riskScore()).isNull();
		assertThat(result.riskLevel()).isEqualTo(RiskLevel.UNDEFINED);
	}
}
