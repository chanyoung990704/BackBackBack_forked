package com.aivle.project.report.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.repository.CompaniesRepository;
import com.aivle.project.common.config.TestSecurityConfig;
import com.aivle.project.metric.entity.MetricValueType;
import com.aivle.project.metric.entity.MetricsEntity;
import com.aivle.project.metric.repository.MetricsRepository;
import com.aivle.project.quarter.entity.QuartersEntity;
import com.aivle.project.quarter.repository.QuartersRepository;
import com.aivle.project.quarter.support.QuarterCalculator;
import com.aivle.project.quarter.support.YearQuarter;
import com.aivle.project.report.dto.ReportPublishResult;
import com.aivle.project.report.entity.CompanyReportMetricValuesEntity;
import com.aivle.project.report.entity.CompanyReportVersionsEntity;
import com.aivle.project.report.entity.CompanyReportsEntity;
import com.aivle.project.report.repository.CompanyReportMetricValuesRepository;
import com.aivle.project.report.repository.CompanyReportVersionsRepository;
import com.aivle.project.report.repository.CompanyReportsRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@Transactional
class CompanyReportMetricPublishServiceTest {

	@Autowired
	private CompanyReportMetricPublishService companyReportMetricPublishService;

	@Autowired
	private CompaniesRepository companiesRepository;

	@Autowired
	private CompanyReportsRepository companyReportsRepository;

	@Autowired
	private CompanyReportVersionsRepository companyReportVersionsRepository;

	@Autowired
	private CompanyReportMetricValuesRepository companyReportMetricValuesRepository;

	@Autowired
	private MetricsRepository metricsRepository;

	@Autowired
	private QuartersRepository quartersRepository;

	@Test
	@DisplayName("보고서 지표를 수동으로 저장한다")
	void publishMetrics_savesValues() {
		// given
		companiesRepository.save(CompaniesEntity.create(
			"00000001",
			"테스트기업",
			"TEST_CO",
			"000020",
			LocalDate.of(2025, 1, 1)
		));
		Map<String, BigDecimal> metrics = new LinkedHashMap<>();
		metrics.put("ROA", new BigDecimal("1.23"));
		metrics.put("OperatingProfitMargin", new BigDecimal("2.34"));
		metrics.put("UNKNOWN", new BigDecimal("9.99"));

		// when
		ReportPublishResult result = companyReportMetricPublishService.publishMetrics(
			"000020",
			20253,
			MetricValueType.PREDICTED,
			metrics
		);

		// then
		assertThat(result.savedValues()).isEqualTo(2);
		assertThat(result.skippedMetrics()).isEqualTo(1);
		assertThat(result.reportVersionNo()).isEqualTo(1);
		assertThat(companyReportsRepository.count()).isEqualTo(1);
		assertThat(companyReportVersionsRepository.count()).isEqualTo(1);
		assertThat(companyReportMetricValuesRepository.count()).isEqualTo(2);
		CompanyReportVersionsEntity version = companyReportVersionsRepository.findAll().get(0);
		assertThat(version.isPublished()).isFalse();
		assertThat(version.getPdfFile()).isNull();

		for (CompanyReportMetricValuesEntity value : companyReportMetricValuesRepository.findAll()) {
			assertThat(value.getValueType()).isEqualTo(MetricValueType.PREDICTED);
		}
	}

	@Test
	@DisplayName("미발행 버전에 동일 타입 값이 없으면 해당 버전을 재사용한다")
	void publishMetrics_reusesUnpublishedVersion() {
		// given
		CompaniesEntity company = companiesRepository.save(CompaniesEntity.create(
			"00000002",
			"다른기업",
			"ANOTHER_CO",
			"000030",
			LocalDate.of(2025, 1, 1)
		));
		YearQuarter yearQuarter = QuarterCalculator.parseQuarterKey(20253);
		QuartersEntity quarter = quartersRepository.save(QuartersEntity.create(
			yearQuarter.year(),
			yearQuarter.quarter(),
			20253,
			QuarterCalculator.startDate(yearQuarter),
			QuarterCalculator.endDate(yearQuarter)
		));
		CompanyReportsEntity report = companyReportsRepository.save(CompanyReportsEntity.create(company, quarter, null));
		CompanyReportVersionsEntity existingVersion = companyReportVersionsRepository.save(CompanyReportVersionsEntity.create(
			report,
			1,
			LocalDateTime.now(),
			false,
			null
		));
		Map<String, BigDecimal> metrics = Map.of("ROA", new BigDecimal("1.23"));

		// when
		ReportPublishResult result = companyReportMetricPublishService.publishMetrics(
			"000030",
			20253,
			MetricValueType.ACTUAL,
			metrics
		);

		// then
		assertThat(result.reportVersionNo()).isEqualTo(1);
		assertThat(companyReportVersionsRepository.count()).isEqualTo(1);
		assertThat(companyReportMetricValuesRepository.count()).isEqualTo(1);
		CompanyReportMetricValuesEntity savedValue = companyReportMetricValuesRepository.findAll().get(0);
		assertThat(savedValue.getReportVersion().getId()).isEqualTo(existingVersion.getId());
	}

	@Test
	@DisplayName("이미 값이 있는 미발행 버전이면 새 버전을 만든다")
	void publishMetrics_createsNewVersionWhenValuesExist() {
		// given
		CompaniesEntity company = companiesRepository.save(CompaniesEntity.create(
			"00000003",
			"세번째기업",
			"THIRD_CO",
			"000040",
			LocalDate.of(2025, 1, 1)
		));
		YearQuarter yearQuarter = QuarterCalculator.parseQuarterKey(20253);
		QuartersEntity quarter = quartersRepository.save(QuartersEntity.create(
			yearQuarter.year(),
			yearQuarter.quarter(),
			20253,
			QuarterCalculator.startDate(yearQuarter),
			QuarterCalculator.endDate(yearQuarter)
		));
		CompanyReportsEntity report = companyReportsRepository.save(CompanyReportsEntity.create(company, quarter, null));
		CompanyReportVersionsEntity existingVersion = companyReportVersionsRepository.save(CompanyReportVersionsEntity.create(
			report,
			1,
			LocalDateTime.now(),
			false,
			null
		));
		MetricsEntity metric = metricsRepository.findByMetricCode("ROA").orElseThrow();
		companyReportMetricValuesRepository.save(CompanyReportMetricValuesEntity.create(
			existingVersion,
			metric,
			quarter,
			new BigDecimal("1.11"),
			MetricValueType.ACTUAL
		));
		Map<String, BigDecimal> metrics = Map.of("ROE", new BigDecimal("2.34"));

		// when
		ReportPublishResult result = companyReportMetricPublishService.publishMetrics(
			"000040",
			20253,
			MetricValueType.ACTUAL,
			metrics
		);

		// then
		assertThat(result.reportVersionNo()).isEqualTo(2);
		assertThat(companyReportVersionsRepository.count()).isEqualTo(2);
	}
}
