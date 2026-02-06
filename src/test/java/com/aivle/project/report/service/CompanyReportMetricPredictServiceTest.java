package com.aivle.project.report.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.repository.CompaniesRepository;
import com.aivle.project.common.config.TestSecurityConfig;
import com.aivle.project.metric.entity.MetricValueType;
import com.aivle.project.quarter.entity.QuartersEntity;
import com.aivle.project.quarter.repository.QuartersRepository;
import com.aivle.project.quarter.support.QuarterCalculator;
import com.aivle.project.quarter.support.YearQuarter;
import com.aivle.project.report.dto.ReportPredictRequest;
import com.aivle.project.report.dto.ReportPredictResult;
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
class CompanyReportMetricPredictServiceTest {

	@Autowired
	private CompanyReportMetricPredictService companyReportMetricPredictService;

	@Autowired
	private CompaniesRepository companiesRepository;

	@Autowired
	private CompanyReportsRepository companyReportsRepository;

	@Autowired
	private CompanyReportVersionsRepository companyReportVersionsRepository;

	@Autowired
	private CompanyReportMetricValuesRepository companyReportMetricValuesRepository;

	@Autowired
	private QuartersRepository quartersRepository;

	@Test
	@DisplayName("예측값 요청을 저장하고 PREDICTED로 적재한다")
	void importPredictedMetrics_savesValues() {
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
		metrics.put("OperatingProfitMargin", null);
		metrics.put("UNKNOWN", new BigDecimal("9.99"));
		ReportPredictRequest request = new ReportPredictRequest("20", 20253, metrics);

		// when
		ReportPredictResult result = companyReportMetricPredictService.importPredictedMetrics(request);

		// then
		assertThat(result.savedValues()).isEqualTo(2);
		assertThat(result.skippedMetrics()).isEqualTo(1);
		assertThat(result.reportVersionNo()).isEqualTo(1);
		assertThat(companyReportsRepository.count()).isEqualTo(1);
		assertThat(companyReportVersionsRepository.count()).isEqualTo(1);
		assertThat(companyReportMetricValuesRepository.count()).isEqualTo(2);

		for (CompanyReportMetricValuesEntity value : companyReportMetricValuesRepository.findAll()) {
			assertThat(value.getValueType()).isEqualTo(MetricValueType.PREDICTED);
		}
	}

	@Test
	@DisplayName("기업이 없으면 저장하지 않고 스킵한다")
	void importPredictedMetrics_skipsMissingCompany() {
		// given
		Map<String, BigDecimal> metrics = Map.of("ROA", new BigDecimal("1.23"));
		ReportPredictRequest request = new ReportPredictRequest("999999", 20253, metrics);

		// when
		ReportPredictResult result = companyReportMetricPredictService.importPredictedMetrics(request);

		// then
		assertThat(result.savedValues()).isZero();
		assertThat(result.skippedCompanies()).isEqualTo(1);
		assertThat(companyReportsRepository.count()).isZero();
		assertThat(companyReportMetricValuesRepository.count()).isZero();
	}

	@Test
	@DisplayName("미발행 버전에 예측값이 없으면 기존 버전을 재사용한다")
	void importPredictedMetrics_reusesUnpublishedVersion() {
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
		ReportPredictRequest request = new ReportPredictRequest("000030", 20253, metrics);

		// when
		ReportPredictResult result = companyReportMetricPredictService.importPredictedMetrics(request);

		// then
		assertThat(result.reportVersionNo()).isEqualTo(1);
		assertThat(companyReportVersionsRepository.count()).isEqualTo(1);
		assertThat(companyReportMetricValuesRepository.findAll().get(0).getReportVersion().getId())
			.isEqualTo(existingVersion.getId());
	}
}
