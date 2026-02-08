package com.aivle.project.report.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@Transactional
class ReportMetricQueryControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CompaniesRepository companiesRepository;

	@Autowired
	private QuartersRepository quartersRepository;

	@Autowired
	private MetricsRepository metricsRepository;

	@Autowired
	private CompanyReportsRepository companyReportsRepository;

	@Autowired
	private CompanyReportVersionsRepository companyReportVersionsRepository;

	@Autowired
	private CompanyReportMetricValuesRepository companyReportMetricValuesRepository;

	@Autowired
	private FilesRepository filesRepository;

	@Test
	@DisplayName("ROLE_USER로 분기별 그룹 지표를 조회한다")
	void fetchGroupedMetrics() throws Exception {
		// given
		CompaniesEntity company = companiesRepository.save(CompaniesEntity.create(
			"00000001",
			"테스트기업",
			"TEST_CO",
			"000020",
			LocalDate.of(2025, 1, 1)
		));
		QuartersEntity q20244 = quartersRepository.save(QuartersEntity.create(
			2024,
			4,
			20244,
			LocalDate.of(2024, 10, 1),
			LocalDate.of(2024, 12, 31)
		));
		QuartersEntity q20253 = quartersRepository.save(QuartersEntity.create(
			2025,
			3,
			20253,
			LocalDate.of(2025, 7, 1),
			LocalDate.of(2025, 9, 30)
		));
		MetricsEntity metric = metricsRepository.findByMetricCode("ROA").orElseThrow();

		CompanyReportsEntity report = companyReportsRepository.save(
			CompanyReportsEntity.create(company, q20253, null)
		);
		CompanyReportVersionsEntity latestVersion = companyReportVersionsRepository.save(
			CompanyReportVersionsEntity.create(report, 1, LocalDateTime.now(), false, null)
		);
		companyReportMetricValuesRepository.save(CompanyReportMetricValuesEntity.create(
			latestVersion,
			metric,
			q20244,
			new BigDecimal("1.11"),
			MetricValueType.ACTUAL
		));
		companyReportMetricValuesRepository.save(CompanyReportMetricValuesEntity.create(
			latestVersion,
			metric,
			q20253,
			new BigDecimal("2.22"),
			MetricValueType.ACTUAL
		));

		// when & then
		mockMvc.perform(get("/api/reports/metrics/grouped")
					.param("stockCode", "000020")
					.param("fromQuarterKey", "20244")
					.param("toQuarterKey", "20253")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.stockCode").value("000020"))
				.andExpect(jsonPath("$.data.quarters.length()").value(2));
	}

	@Test
	@DisplayName("분기별 그룹 지표 조회는 인증이 없으면 401을 반환한다")
	void fetchGroupedMetrics_unauthorized() throws Exception {
		// when & then
		mockMvc.perform(get("/api/reports/metrics/grouped")
				.param("stockCode", "000020")
				.param("fromQuarterKey", "20244")
				.param("toQuarterKey", "20253"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("분기별 그룹 지표 조회는 ROLE_ADMIN도 접근 가능하다 (권한 계층)")
	void fetchGroupedMetrics_allowedForAdmin() throws Exception {
		// when & then
		mockMvc.perform(get("/api/reports/metrics/grouped")
				.param("stockCode", "000020")
				.param("fromQuarterKey", "20244")
				.param("toQuarterKey", "20253")
				.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("ROLE_USER로 최신 예측 지표와 PDF 정보를 조회한다")
	void fetchLatestPredictMetrics() throws Exception {
		// given
		CompaniesEntity company = companiesRepository.save(CompaniesEntity.create(
			"00000002",
			"예측기업",
			"PREDICT_CO",
			"000020",
			LocalDate.of(2025, 1, 1)
		));
		QuartersEntity q20253 = quartersRepository.save(QuartersEntity.create(
			2025,
			3,
			20253,
			LocalDate.of(2025, 7, 1),
			LocalDate.of(2025, 9, 30)
		));
		MetricsEntity metric = metricsRepository.findByMetricCode("ROA").orElseThrow();

		CompanyReportsEntity report = companyReportsRepository.save(
			CompanyReportsEntity.create(company, q20253, null)
		);
		CompanyReportVersionsEntity latestVersion = companyReportVersionsRepository.save(
			CompanyReportVersionsEntity.create(report, 1, LocalDateTime.now(), false, null)
		);
		FilesEntity pdf = filesRepository.save(FilesEntity.create(
			FileUsageType.REPORT_PDF,
			"http://example.com/report.pdf",
			null,
			"report.pdf",
			1200L,
			"application/pdf"
		));
		latestVersion.publishWithPdf(pdf);
		companyReportVersionsRepository.save(latestVersion);
		companyReportMetricValuesRepository.save(CompanyReportMetricValuesEntity.create(
			latestVersion,
			metric,
			q20253,
			new BigDecimal("9.99"),
			MetricValueType.PREDICTED
		));

		// when & then
		mockMvc.perform(get("/api/reports/metrics/predict-latest")
					.param("stockCode", "000020")
					.param("quarterKey", "20253")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.stockCode").value("000020"))
				.andExpect(jsonPath("$.data.versionNo").value(1))
				.andExpect(jsonPath("$.data.pdfFileId").value(pdf.getId()))
				.andExpect(jsonPath("$.data.metrics.length()").value(1));
	}

	@Test
	@DisplayName("최신 예측 지표 조회는 인증이 없으면 401을 반환한다")
	void fetchLatestPredictMetrics_unauthorized() throws Exception {
		// when & then
		mockMvc.perform(get("/api/reports/metrics/predict-latest")
				.param("stockCode", "000020")
				.param("quarterKey", "20253"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("최신 예측 지표 조회는 ROLE_ADMIN도 접근 가능하다 (권한 계층)")
	void fetchLatestPredictMetrics_allowedForAdmin() throws Exception {
		// when & then
		mockMvc.perform(get("/api/reports/metrics/predict-latest")
				.param("stockCode", "000020")
				.param("quarterKey", "20253")
				.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
			.andExpect(status().isOk());
	}
}