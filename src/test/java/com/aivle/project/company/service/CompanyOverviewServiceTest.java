package com.aivle.project.company.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.aivle.project.common.config.QuerydslConfig;
import com.aivle.project.company.client.AiServerClient;
import com.aivle.project.company.dto.CompanyOverviewResponseDto;
import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.keymetric.entity.CompanyKeyMetricEntity;
import com.aivle.project.company.keymetric.entity.CompanyKeyMetricRiskLevel;
import com.aivle.project.company.keymetric.entity.KeyMetricDescriptionEntity;
import com.aivle.project.company.keymetric.repository.CompanyKeyMetricRepository;
import com.aivle.project.company.keymetric.repository.KeyMetricDescriptionRepository;
import com.aivle.project.company.repository.CompaniesRepository;
import com.aivle.project.industry.entity.IndustryEntity;
import com.aivle.project.industry.entity.IndustryRepository;
import com.aivle.project.metric.entity.MetricValueType;
import com.aivle.project.metric.entity.MetricsEntity;
import com.aivle.project.metric.repository.MetricDescriptionRepository;
import com.aivle.project.metric.repository.MetricsRepository;
import com.aivle.project.metric.entity.MetricDescriptionEntity;
import com.aivle.project.quarter.entity.QuartersEntity;
import com.aivle.project.quarter.repository.QuartersRepository;
import com.aivle.project.quarter.support.QuarterCalculator;
import com.aivle.project.quarter.support.YearQuarter;
import com.aivle.project.report.entity.CompanyReportMetricValuesEntity;
import com.aivle.project.report.entity.CompanyReportVersionsEntity;
import com.aivle.project.report.entity.CompanyReportsEntity;
import com.aivle.project.report.entity.SignalColor;
import com.aivle.project.report.repository.CompanyReportMetricValuesRepository;
import com.aivle.project.report.repository.CompanyReportVersionsRepository;
import com.aivle.project.report.repository.CompanyReportsRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import({
	QuerydslConfig.class,
	CompanyOverviewService.class,
	CompanyInfoService.class,
	CompanySectorService.class,
	CompanyHealthScoreCacheService.class,
	CompanyPredictionCacheService.class
})
class CompanyOverviewServiceTest {

	@Autowired
	private CompanyOverviewService companyOverviewService;

	@Autowired
	private CompaniesRepository companiesRepository;

	@Autowired
	private IndustryRepository industryRepository;

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
	private MetricDescriptionRepository metricDescriptionRepository;

	@Autowired
	private KeyMetricDescriptionRepository keyMetricDescriptionRepository;

	@Autowired
	private CompanyKeyMetricRepository companyKeyMetricRepository;

	@MockBean
	private AiServerClient aiServerClient;

	@MockBean
	private CompanySignalCacheService companySignalCacheService;

	@MockBean
	private CompanyReputationScoreService companyReputationScoreService;

	@Test
	@DisplayName("기업 개요 응답을 구성한다")
	void getOverview() {
		// given
		IndustryEntity industry = industryRepository.save(IndustryEntity.create("A0101", "식품"));
		CompaniesEntity company = companiesRepository.save(CompaniesEntity.create(
			"00000001",
			"테스트기업",
			"TEST_CO",
			"000020",
			LocalDate.of(2025, 1, 1),
			industry
		));

		int quarterKey = 20251;
		YearQuarter yearQuarter = QuarterCalculator.parseQuarterKey(quarterKey);
		QuartersEntity quarter = quartersRepository.save(QuartersEntity.create(
			yearQuarter.year(),
			yearQuarter.quarter(),
			quarterKey,
			QuarterCalculator.startDate(yearQuarter),
			QuarterCalculator.endDate(yearQuarter)
		));

		int nextQuarterKey = QuarterCalculator.offset(yearQuarter, 1).toQuarterKey();
		YearQuarter nextYearQuarter = QuarterCalculator.parseQuarterKey(nextQuarterKey);
		QuartersEntity nextQuarter = quartersRepository.save(QuartersEntity.create(
			nextYearQuarter.year(),
			nextYearQuarter.quarter(),
			nextQuarterKey,
			QuarterCalculator.startDate(nextYearQuarter),
			QuarterCalculator.endDate(nextYearQuarter)
		));

		CompanyReportsEntity report = companyReportsRepository.save(CompanyReportsEntity.create(company, quarter, null));
		CompanyReportVersionsEntity version = companyReportVersionsRepository.save(CompanyReportVersionsEntity.create(
			report,
			1,
			LocalDateTime.now(),
			true,
			null
		));
		CompanyReportsEntity nextReport = companyReportsRepository.save(CompanyReportsEntity.create(company, nextQuarter, null));
		CompanyReportVersionsEntity nextVersion = companyReportVersionsRepository.save(CompanyReportVersionsEntity.create(
			nextReport,
			1,
			LocalDateTime.now(),
			true,
			null
		));

		MetricsEntity metric = metricsRepository.save(MetricsEntity.create(
			"REVENUE",
			"매출",
			"REVENUE",
			false
		));
		metricDescriptionRepository.save(MetricDescriptionEntity.create(
			metric,
			"매출 설명",
			"매출 해석",
			"매출 힌트",
			"ko"
		));

		CompanyReportMetricValuesEntity actualValue = CompanyReportMetricValuesEntity.create(
			version,
			metric,
			quarter,
			BigDecimal.valueOf(120.5),
			MetricValueType.ACTUAL
		);
		actualValue.applySignal(SignalColor.GREEN, "양호", BigDecimal.valueOf(100));
		companyReportMetricValuesRepository.save(actualValue);

		CompanyReportMetricValuesEntity predictedValue = CompanyReportMetricValuesEntity.create(
			nextVersion,
			metric,
			nextQuarter,
			BigDecimal.valueOf(130.5),
			MetricValueType.PREDICTED
		);
		predictedValue.applySignal(SignalColor.GREEN, "양호", BigDecimal.valueOf(110));
		companyReportMetricValuesRepository.save(predictedValue);

		keyMetricDescriptionRepository.findByMetricCode("NETWORK_HEALTH")
			.orElseGet(() -> keyMetricDescriptionRepository.save(KeyMetricDescriptionEntity.create(
				"NETWORK_HEALTH",
				"내부 건강도",
				"%",
				"내부 설명",
				"내부 해석",
				"내부 힌트",
				BigDecimal.valueOf(0.6),
				BigDecimal.valueOf(70),
				BigDecimal.valueOf(40),
				true
			)));
		keyMetricDescriptionRepository.findByMetricCode("EXTERNAL_HEALTH")
			.orElseGet(() -> keyMetricDescriptionRepository.save(KeyMetricDescriptionEntity.create(
				"EXTERNAL_HEALTH",
				"외부 건강도",
				"%",
				"외부 설명",
				"외부 해석",
				"외부 힌트",
				BigDecimal.valueOf(0.4),
				BigDecimal.valueOf(70),
				BigDecimal.valueOf(40),
				true
			)));

		companyKeyMetricRepository.save(CompanyKeyMetricEntity.create(
			company,
			quarter,
			version,
			BigDecimal.valueOf(82.5),
			BigDecimal.valueOf(61.2),
			BigDecimal.valueOf(70.1),
			CompanyKeyMetricRiskLevel.SAFE,
			1,
			LocalDateTime.now()
		));

		// when
		CompanyOverviewResponseDto result = companyOverviewService.getOverview(company.getId(), String.valueOf(quarterKey));

		// then
		assertThat(result.getCompany().getName()).isEqualTo("테스트기업");
		assertThat(result.getCompany().getStockCode()).isEqualTo("000020");
		assertThat(result.getForecast().getLatestActualQuarter()).isEqualTo("2025Q1");
		assertThat(result.getForecast().getNextQuarter()).isEqualTo("2025Q2");
		assertThat(result.getForecast().getMetricSeries()).hasSize(1);
		assertThat(result.getForecast().getMetricSeries().get(0).getPoints()).hasSize(2);
		assertThat(result.getKeyMetrics()).hasSize(3);
		assertThat(result.getKeyMetrics().get(0).getLabel()).isEqualTo("내부 건강도");
		assertThat(result.getSignals()).hasSize(1);
		assertThat(result.getSignals().get(0).getLevel().name()).isEqualTo("GREEN");
	}

	@Test
	@DisplayName("기업 개요 지표는 위험/비위험 지표의 최신 버전을 함께 조회한다")
	void getOverviewWithRiskAndNonRiskLatestVersions() {
		// given
		IndustryEntity industry = industryRepository.save(IndustryEntity.create("A0101", "식품"));
		CompaniesEntity company = companiesRepository.save(CompaniesEntity.create(
			"00000002",
			"테스트기업2",
			"TEST_CO2",
			"000030",
			LocalDate.of(2025, 1, 1),
			industry
		));

		int quarterKey = 20251;
		YearQuarter yearQuarter = QuarterCalculator.parseQuarterKey(quarterKey);
		QuartersEntity quarter = quartersRepository.save(QuartersEntity.create(
			yearQuarter.year(),
			yearQuarter.quarter(),
			quarterKey,
			QuarterCalculator.startDate(yearQuarter),
			QuarterCalculator.endDate(yearQuarter)
		));

		CompanyReportsEntity report = companyReportsRepository.save(CompanyReportsEntity.create(company, quarter, null));
		CompanyReportVersionsEntity riskVersion = companyReportVersionsRepository.save(CompanyReportVersionsEntity.create(
			report,
			1,
			LocalDateTime.now().minusDays(1),
			true,
			null
		));
		CompanyReportVersionsEntity nonRiskVersion = companyReportVersionsRepository.save(CompanyReportVersionsEntity.create(
			report,
			2,
			LocalDateTime.now(),
			true,
			null
		));

		MetricsEntity riskMetric = metricsRepository.save(MetricsEntity.create(
			"RISK_METRIC",
			"위험지표",
			"RISK_METRIC",
			true
		));
		MetricsEntity nonRiskMetric = metricsRepository.save(MetricsEntity.create(
			"SAFE_METRIC",
			"비위험지표",
			"SAFE_METRIC",
			false
		));

		int nextQuarterKey = QuarterCalculator.offset(yearQuarter, 1).toQuarterKey();
		YearQuarter nextYearQuarter = QuarterCalculator.parseQuarterKey(nextQuarterKey);
		QuartersEntity nextQuarter = quartersRepository.save(QuartersEntity.create(
			nextYearQuarter.year(),
			nextYearQuarter.quarter(),
			nextQuarterKey,
			QuarterCalculator.startDate(nextYearQuarter),
			QuarterCalculator.endDate(nextYearQuarter)
		));

		CompanyReportMetricValuesEntity riskValue = CompanyReportMetricValuesEntity.create(
			riskVersion,
			riskMetric,
			nextQuarter,
			BigDecimal.valueOf(10.5),
			MetricValueType.PREDICTED
		);
		riskValue.applySignal(SignalColor.RED, "위험", BigDecimal.valueOf(15));
		companyReportMetricValuesRepository.save(riskValue);

		CompanyReportMetricValuesEntity nonRiskValue = CompanyReportMetricValuesEntity.create(
			nonRiskVersion,
			nonRiskMetric,
			nextQuarter,
			BigDecimal.valueOf(20.5),
			MetricValueType.PREDICTED
		);
		nonRiskValue.applySignal(SignalColor.GREEN, "양호", BigDecimal.valueOf(25));
		companyReportMetricValuesRepository.save(nonRiskValue);

		// when
		CompanyOverviewResponseDto result = companyOverviewService.getOverview(company.getId(), String.valueOf(quarterKey));

		// then
		assertThat(result.getForecast().getMetricSeries())
			.extracting("key")
			.containsExactlyInAnyOrder("RISK_METRIC", "SAFE_METRIC");
		assertThat(result.getSignals())
			.extracting("key")
			.containsExactlyInAnyOrder("RISK_METRIC", "SAFE_METRIC");
		assertThat(result.getSignals().stream()
			.filter(metric -> "RISK_METRIC".equals(metric.getKey()))
			.findFirst()
			.orElseThrow()
			.getLevel()
			.name())
			.isEqualTo("RED");
	}

	@Test
	@DisplayName("quarterKey가 없으면 ACTUAL 최신 분기를 기준으로 개요를 구성한다")
	void getOverview_shouldUseLatestActualQuarterWhenMissingQuarterKey() {
		// given
		IndustryEntity industry = industryRepository.save(IndustryEntity.create("A0101", "식품"));
		CompaniesEntity company = companiesRepository.save(CompaniesEntity.create(
			"00000003",
			"테스트기업3",
			"TEST_CO3",
			"000040",
			LocalDate.of(2025, 1, 1),
			industry
		));

		int quarterKey = 20251;
		YearQuarter yearQuarter = QuarterCalculator.parseQuarterKey(quarterKey);
		QuartersEntity quarter = quartersRepository.save(QuartersEntity.create(
			yearQuarter.year(),
			yearQuarter.quarter(),
			quarterKey,
			QuarterCalculator.startDate(yearQuarter),
			QuarterCalculator.endDate(yearQuarter)
		));

		CompanyReportsEntity report = companyReportsRepository.save(CompanyReportsEntity.create(company, quarter, null));
		CompanyReportVersionsEntity version = companyReportVersionsRepository.save(CompanyReportVersionsEntity.create(
			report,
			1,
			LocalDateTime.now(),
			true,
			null
		));

		MetricsEntity metric = metricsRepository.save(MetricsEntity.create(
			"REVENUE2",
			"매출2",
			"REVENUE2",
			false
		));
		metricDescriptionRepository.save(MetricDescriptionEntity.create(
			metric,
			"매출 설명",
			"매출 해석",
			"매출 힌트",
			"ko"
		));

		CompanyReportMetricValuesEntity actualValue = CompanyReportMetricValuesEntity.create(
			version,
			metric,
			quarter,
			BigDecimal.valueOf(120.5),
			MetricValueType.ACTUAL
		);
		actualValue.applySignal(SignalColor.GREEN, "양호", BigDecimal.valueOf(100));
		companyReportMetricValuesRepository.save(actualValue);

		// when
		CompanyOverviewResponseDto result = companyOverviewService.getOverview(company.getId(), null);

		// then
		assertThat(result.getForecast().getLatestActualQuarter()).isEqualTo("2025Q1");
	}
}
