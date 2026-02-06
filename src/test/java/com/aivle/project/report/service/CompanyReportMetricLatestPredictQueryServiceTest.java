package com.aivle.project.report.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.repository.CompaniesRepository;
import com.aivle.project.common.config.TestSecurityConfig;
import com.aivle.project.file.entity.FileUsageType;
import com.aivle.project.file.entity.FilesEntity;
import com.aivle.project.file.repository.FilesRepository;
import com.aivle.project.metric.entity.MetricValueType;
import com.aivle.project.metric.entity.MetricsEntity;
import com.aivle.project.metric.repository.MetricsRepository;
import com.aivle.project.quarter.entity.QuartersEntity;
import com.aivle.project.quarter.repository.QuartersRepository;
import com.aivle.project.quarter.support.QuarterCalculator;
import com.aivle.project.quarter.support.YearQuarter;
import com.aivle.project.report.dto.ReportLatestPredictResponse;
import com.aivle.project.report.entity.CompanyReportMetricValuesEntity;
import com.aivle.project.report.entity.CompanyReportVersionsEntity;
import com.aivle.project.report.entity.CompanyReportsEntity;
import com.aivle.project.report.repository.CompanyReportMetricValuesRepository;
import com.aivle.project.report.repository.CompanyReportVersionsRepository;
import com.aivle.project.report.repository.CompanyReportsRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
class CompanyReportMetricLatestPredictQueryServiceTest {

	@Autowired
	private CompanyReportMetricQueryService companyReportMetricQueryService;

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
	private FilesRepository filesRepository;

	@Test
	@DisplayName("최신 버전의 PREDICT 지표와 PDF 정보를 조회한다")
	void fetchLatestPredictMetrics_returnsPredictValuesAndPdf() {
		// given
		CompaniesEntity company = companiesRepository.save(CompaniesEntity.create(
			"00000001",
			"테스트기업",
			"TEST_CO",
			"000020",
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
		CompanyReportVersionsEntity version = companyReportVersionsRepository.save(CompanyReportVersionsEntity.create(
			report,
			1,
			LocalDateTime.now(),
			false,
			null
		));
		FilesEntity pdf = filesRepository.save(FilesEntity.create(
			FileUsageType.REPORT_PDF,
			"http://example.com/report.pdf",
			null,
			"report.pdf",
			1200L,
			"application/pdf"
		));
		version.publishWithPdf(pdf);
		companyReportVersionsRepository.save(version);

		MetricsEntity roa = metricsRepository.findByMetricCode("ROA").orElseThrow();
		MetricsEntity opMargin = metricsRepository.findByMetricCode("OpMargin").orElseThrow();

		companyReportMetricValuesRepository.save(CompanyReportMetricValuesEntity.create(
			version,
			roa,
			quarter,
			new BigDecimal("1.23"),
			MetricValueType.PREDICTED
		));
		companyReportMetricValuesRepository.save(CompanyReportMetricValuesEntity.create(
			version,
			opMargin,
			quarter,
			new BigDecimal("2.34"),
			MetricValueType.PREDICTED
		));

		// when
		ReportLatestPredictResponse response = companyReportMetricQueryService
			.fetchLatestPredictMetrics("20", 20253);

		// then
		assertThat(response.stockCode()).isEqualTo("000020");
		assertThat(response.versionNo()).isEqualTo(1);
		assertThat(response.pdfFileId()).isEqualTo(pdf.getId());
		assertThat(response.metrics()).hasSize(2);
	}

	@Test
	@DisplayName("최신 버전에 PREDICT가 없으면 최신 PREDICT 버전을 조회한다")
	void fetchLatestPredictMetrics_fallbackToLatestPredictVersion() {
		// given
		CompaniesEntity company = companiesRepository.save(CompaniesEntity.create(
			"00000002",
			"테스트기업2",
			"TEST_CO2",
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
		CompanyReportVersionsEntity predictVersion = companyReportVersionsRepository.save(CompanyReportVersionsEntity.create(
			report,
			1,
			LocalDateTime.now().minusDays(1),
			false,
			null
		));
		companyReportVersionsRepository.save(CompanyReportVersionsEntity.create(
			report,
			2,
			LocalDateTime.now(),
			false,
			null
		));

		MetricsEntity roa = metricsRepository.findByMetricCode("ROA").orElseThrow();
		companyReportMetricValuesRepository.save(CompanyReportMetricValuesEntity.create(
			predictVersion,
			roa,
			quarter,
			new BigDecimal("4.56"),
			MetricValueType.PREDICTED
		));

		// when
		ReportLatestPredictResponse response = companyReportMetricQueryService
			.fetchLatestPredictMetrics("30", 20253);

		// then
		assertThat(response.versionNo()).isEqualTo(1);
		assertThat(response.metrics()).hasSize(1);
		assertThat(response.metrics().get(0).metricValue()).isEqualByComparingTo("4.56");
	}
}
