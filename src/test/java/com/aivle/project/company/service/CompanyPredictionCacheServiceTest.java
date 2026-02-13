package com.aivle.project.company.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import com.aivle.project.common.config.QuerydslConfig;
import com.aivle.project.company.client.AiServerClient;
import com.aivle.project.company.dto.AiAnalysisResponse;
import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.repository.CompaniesRepository;
import com.aivle.project.industry.entity.IndustryEntity;
import com.aivle.project.industry.entity.IndustryRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import({QuerydslConfig.class, CompanyPredictionCacheService.class, CompanyReportVersionIssueService.class})
class CompanyPredictionCacheServiceTest {

	@Autowired
	private CompanyPredictionCacheService companyPredictionCacheService;

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

	@MockBean
	private AiServerClient aiServerClient;

	@Test
	@DisplayName("최신 ACTUAL 분기 요청 시 예측값이 없으면 AI 호출 후 최신 버전에 저장한다")
	void ensurePredictionCached() {
		// given
		IndustryEntity industry = industryRepository.save(IndustryEntity.create("A0101", "식품"));
		CompaniesEntity company = companiesRepository.save(CompaniesEntity.create(
			"00000001",
			"테스트기업",
			"TEST_CO",
			"005930",
			LocalDate.of(2025, 1, 1),
			industry
		));

		int latestActualKey = 20253;
		YearQuarter actualQuarter = QuarterCalculator.parseQuarterKey(latestActualKey);
		QuartersEntity actual = quartersRepository.save(QuartersEntity.create(
			actualQuarter.year(),
			actualQuarter.quarter(),
			latestActualKey,
			QuarterCalculator.startDate(actualQuarter),
			QuarterCalculator.endDate(actualQuarter)
		));

		YearQuarter nextQuarter = QuarterCalculator.offset(actualQuarter, 1);
		QuartersEntity target = quartersRepository.save(QuartersEntity.create(
			nextQuarter.year(),
			nextQuarter.quarter(),
			nextQuarter.toQuarterKey(),
			QuarterCalculator.startDate(nextQuarter),
			QuarterCalculator.endDate(nextQuarter)
		));

		CompanyReportsEntity report = companyReportsRepository.save(CompanyReportsEntity.create(company, target, null));
		CompanyReportVersionsEntity version = companyReportVersionsRepository.save(CompanyReportVersionsEntity.create(
			report,
			2,
			LocalDateTime.now(),
			true,
			null
		));

		MetricsEntity metric = metricsRepository.findByMetricCode("ROA")
			.orElseGet(() -> metricsRepository.save(MetricsEntity.create(
				"ROA",
				"ROA",
				"ROA",
				false
			)));

		companyReportMetricValuesRepository.save(CompanyReportMetricValuesEntity.create(
			version,
			metric,
			actual,
			BigDecimal.valueOf(1.0),
			com.aivle.project.metric.entity.MetricValueType.ACTUAL
		));

		given(aiServerClient.getPrediction(company.getStockCode()))
			.willReturn(new AiAnalysisResponse(
				company.getStockCode(),
				company.getCorpName(),
				String.valueOf(latestActualKey),
				Map.of("ROA", 1.23)
			));

		// when
		companyPredictionCacheService.ensurePredictionCached(company.getId(), latestActualKey);

		// then
		var predictions = companyReportMetricValuesRepository
			.findLatestMetricsByStockCodeAndQuarterKeyAndType(
				company.getStockCode(),
				nextQuarter.toQuarterKey(),
				com.aivle.project.metric.entity.MetricValueType.PREDICTED
			);
		assertThat(predictions).hasSize(1);
		assertThat(predictions.get(0).getVersionNo()).isEqualTo(version.getVersionNo());
	}

	@Test
	@DisplayName("요청 분기가 최신 ACTUAL이 아니면 AI 호출을 생략한다")
	void skipPredictionWhenQuarterNotLatestActual() {
		// given
		IndustryEntity industry = industryRepository.save(IndustryEntity.create("A0101", "식품"));
		CompaniesEntity company = companiesRepository.save(CompaniesEntity.create(
			"00000002",
			"테스트기업2",
			"TEST_CO2",
			"000020",
			LocalDate.of(2025, 1, 1),
			industry
		));

		int latestActualKey = 20253;
		YearQuarter actualQuarter = QuarterCalculator.parseQuarterKey(latestActualKey);
		QuartersEntity actual = quartersRepository.save(QuartersEntity.create(
			actualQuarter.year(),
			actualQuarter.quarter(),
			latestActualKey,
			QuarterCalculator.startDate(actualQuarter),
			QuarterCalculator.endDate(actualQuarter)
		));

		CompanyReportsEntity report = companyReportsRepository.save(CompanyReportsEntity.create(company, actual, null));
		CompanyReportVersionsEntity version = companyReportVersionsRepository.save(CompanyReportVersionsEntity.create(
			report,
			1,
			LocalDateTime.now(),
			true,
			null
		));

		MetricsEntity metric = metricsRepository.findByMetricCode("ROA")
			.orElseGet(() -> metricsRepository.save(MetricsEntity.create(
				"ROA",
				"ROA",
				"ROA",
				false
			)));

		companyReportMetricValuesRepository.save(CompanyReportMetricValuesEntity.create(
			version,
			metric,
			actual,
			BigDecimal.valueOf(1.0),
			com.aivle.project.metric.entity.MetricValueType.ACTUAL
		));

		// when
		companyPredictionCacheService.ensurePredictionCached(company.getId(), 20251);

		// then
		verifyNoInteractions(aiServerClient);
	}
}
