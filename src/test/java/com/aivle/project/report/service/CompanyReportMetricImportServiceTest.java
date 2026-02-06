package com.aivle.project.report.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.repository.CompaniesRepository;
import com.aivle.project.common.config.TestSecurityConfig;
import com.aivle.project.quarter.entity.QuartersEntity;
import com.aivle.project.quarter.repository.QuartersRepository;
import com.aivle.project.report.dto.CompanyMetricValueCommand;
import com.aivle.project.report.dto.ReportImportResult;
import com.aivle.project.report.entity.CompanyReportVersionsEntity;
import com.aivle.project.report.entity.CompanyReportsEntity;
import com.aivle.project.report.repository.CompanyReportMetricValuesRepository;
import com.aivle.project.report.repository.CompanyReportVersionsRepository;
import com.aivle.project.report.repository.CompanyReportsRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
class CompanyReportMetricImportServiceTest {

	@Autowired
	private CompanyReportMetricImportService companyReportMetricImportService;

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

	@Test
	@DisplayName("분기 보고서 버전과 지표 값을 생성한다")
	void importMetrics_createsReportVersionAndValues() {
		// given
		CompaniesEntity company = companiesRepository.save(CompaniesEntity.create(
			"00000001",
			"테스트기업",
			"TEST_CO",
			"000020",
			LocalDate.of(2025, 1, 1)
		));
		List<CompanyMetricValueCommand> commands = List.of(
			new CompanyMetricValueCommand("20", "ROA", 0, new BigDecimal("1.23"), 2, 3, "ROA_현재"),
			new CompanyMetricValueCommand("000020", "ROE", -1, new BigDecimal("2.34"), 2, 7, "ROE_분기-1")
		);

		// when
		ReportImportResult result = companyReportMetricImportService.importMetrics(20253, commands);

		// then
		assertThat(result.savedValues()).isEqualTo(2);
		QuartersEntity baseQuarter = quartersRepository.findByQuarterKey(20253).orElseThrow();
		QuartersEntity previousQuarter = quartersRepository.findByQuarterKey(20252).orElseThrow();
		CompanyReportsEntity report = companyReportsRepository.findByCompanyIdAndQuarterId(
			company.getId(),
			baseQuarter.getId()
		).orElseThrow();
		CompanyReportsEntity previousQuarterReport = companyReportsRepository.findByCompanyIdAndQuarterId(
			company.getId(),
			previousQuarter.getId()
		).orElseThrow();
		CompanyReportVersionsEntity version = companyReportVersionsRepository
			.findTopByCompanyReportOrderByVersionNoDesc(report)
			.orElseThrow();
		CompanyReportVersionsEntity previousQuarterVersion = companyReportVersionsRepository
			.findTopByCompanyReportOrderByVersionNoDesc(previousQuarterReport)
			.orElseThrow();

		assertThat(version.getVersionNo()).isEqualTo(1);
		assertThat(previousQuarterVersion.getVersionNo()).isEqualTo(1);
		assertThat(companyReportsRepository.count()).isEqualTo(2);
		assertThat(companyReportMetricValuesRepository.count()).isEqualTo(2);
	}

	@Test
	@DisplayName("기업 또는 지표가 없으면 스킵하고 유효 값만 저장한다")
	void importMetrics_skipsMissingCompanyAndMetric() {
		// given
		companiesRepository.save(CompaniesEntity.create(
			"00000002",
			"다른기업",
			"ANOTHER_CO",
			"000030",
			LocalDate.of(2025, 1, 1)
		));
		List<CompanyMetricValueCommand> commands = List.of(
			new CompanyMetricValueCommand("000030", "ROA", 0, new BigDecimal("3.21"), 2, 3, "ROA_현재"),
			new CompanyMetricValueCommand("000030", "UNKNOWN_METRIC", 0, new BigDecimal("4.56"), 2, 4, "UNK_현재"),
			new CompanyMetricValueCommand("999999", "ROA", 0, new BigDecimal("7.89"), 3, 3, "ROA_현재")
		);

		// when
		ReportImportResult result = companyReportMetricImportService.importMetrics(20253, commands);

		// then
		assertThat(result.savedValues()).isEqualTo(1);
		assertThat(result.skippedCompanies()).isEqualTo(1);
		assertThat(result.skippedMetrics()).isEqualTo(1);
		assertThat(companyReportMetricValuesRepository.count()).isEqualTo(1);
	}

	@Test
	@DisplayName("미발행 버전에 ACTUAL 값이 없으면 기존 버전에 적재한다")
	void importMetrics_reusesUnpublishedVersion() {
		// given
		CompaniesEntity company = companiesRepository.save(CompaniesEntity.create(
			"00000003",
			"세번째기업",
			"THIRD_CO",
			"000040",
			LocalDate.of(2025, 1, 1)
		));
		QuartersEntity quarter = quartersRepository.save(QuartersEntity.create(
			2025,
			3,
			20253,
			com.aivle.project.quarter.support.QuarterCalculator.startDate(
				com.aivle.project.quarter.support.QuarterCalculator.parseQuarterKey(20253)),
			com.aivle.project.quarter.support.QuarterCalculator.endDate(
				com.aivle.project.quarter.support.QuarterCalculator.parseQuarterKey(20253))
		));
		CompanyReportsEntity report = companyReportsRepository.save(CompanyReportsEntity.create(company, quarter, null));
		CompanyReportVersionsEntity existingVersion = companyReportVersionsRepository.save(CompanyReportVersionsEntity.create(
			report,
			1,
			LocalDateTime.now(),
			false,
			null
		));
		List<CompanyMetricValueCommand> commands = List.of(
			new CompanyMetricValueCommand("000040", "ROA", 0, new BigDecimal("1.23"), 2, 3, "ROA_현재")
		);

		// when
		ReportImportResult result = companyReportMetricImportService.importMetrics(20253, commands);

		// then
		assertThat(result.savedValues()).isEqualTo(1);
		assertThat(companyReportVersionsRepository.count()).isEqualTo(1);
		assertThat(companyReportMetricValuesRepository.findAll().get(0).getReportVersion().getId())
			.isEqualTo(existingVersion.getId());
	}
}
