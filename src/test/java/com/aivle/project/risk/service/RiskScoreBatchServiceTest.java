package com.aivle.project.risk.service;

import static org.assertj.core.api.Assertions.assertThat;

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
import com.aivle.project.risk.entity.RiskScoreSummaryEntity;
import com.aivle.project.risk.entity.RiskLevel;
import com.aivle.project.risk.repository.RiskScoreSummaryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import({RiskScoreCalculationService.class, RiskScoreBatchService.class})
class RiskScoreBatchServiceTest {

	@Autowired
	private RiskScoreBatchService riskScoreBatchService;

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
	@DisplayName("모든 기업-분기의 최신 보고서 버전만 배치 계산해 저장한다")
	void calculateAndUpsertAllLatest() {
		// given
		QuartersEntity quarter = quartersRepository.save(QuartersEntity.create(
			2025, 4, 20254, LocalDate.of(2025, 10, 1), LocalDate.of(2025, 12, 31)
		));
		MetricsEntity roa = metricsRepository.findByMetricCode("ROA").orElseThrow();
		jdbcTemplate.update("UPDATE metrics SET is_risk_indicator = 1 WHERE id = ?", roa.getId());

		CompanyReportVersionsEntity latestA = createReportWithVersions("10000001", "기업A", quarter, roa, new BigDecimal("80"), new BigDecimal("20"));
		CompanyReportVersionsEntity latestB = createReportWithVersions("10000002", "기업B", quarter, roa, new BigDecimal("55"), null);

		// when
		int processed = riskScoreBatchService.calculateAndUpsertAllLatest();

		// then
		assertThat(processed).isEqualTo(2);
		List<RiskScoreSummaryEntity> summaries = riskScoreSummaryRepository.findAll();
		assertThat(summaries).hasSize(2);
		assertThat(summaries)
			.extracting(summary -> summary.getReportVersion().getId())
			.containsExactlyInAnyOrder(latestA.getId(), latestB.getId());
		assertThat(summaries)
			.extracting(RiskScoreSummaryEntity::getRiskLevel)
			.containsExactlyInAnyOrder(RiskLevel.DANGER, RiskLevel.CAUTION);
	}

	private CompanyReportVersionsEntity createReportWithVersions(
		String corpCode,
		String corpName,
		QuartersEntity quarter,
		MetricsEntity riskMetric,
		BigDecimal latestValue,
		BigDecimal oldValue
	) {
		CompaniesEntity company = companiesRepository.save(CompaniesEntity.create(
			corpCode, corpName, corpName + "ENG", corpCode.substring(2), LocalDate.of(2025, 1, 1)
		));
		CompanyReportsEntity report = companyReportsRepository.save(CompanyReportsEntity.create(company, quarter, null));

		if (oldValue != null) {
			CompanyReportVersionsEntity oldVersion = companyReportVersionsRepository.save(
				CompanyReportVersionsEntity.create(report, 1, LocalDateTime.now().minusDays(1), true, null)
			);
			companyReportMetricValuesRepository.save(CompanyReportMetricValuesEntity.create(
				oldVersion, riskMetric, quarter, oldValue, MetricValueType.ACTUAL
			));
		}

		CompanyReportVersionsEntity latestVersion = companyReportVersionsRepository.save(
			CompanyReportVersionsEntity.create(report, 2, LocalDateTime.now(), true, null)
		);
		companyReportMetricValuesRepository.save(CompanyReportMetricValuesEntity.create(
			latestVersion, riskMetric, quarter, latestValue, MetricValueType.ACTUAL
		));
		return latestVersion;
	}
}
