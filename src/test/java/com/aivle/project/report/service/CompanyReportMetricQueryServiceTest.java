package com.aivle.project.report.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.aivle.project.common.config.QuerydslConfig;
import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.repository.CompaniesRepository;
import com.aivle.project.metric.entity.MetricValueType;
import com.aivle.project.metric.entity.MetricsEntity;
import com.aivle.project.metric.repository.MetricsRepository;
import com.aivle.project.quarter.entity.QuartersEntity;
import com.aivle.project.quarter.repository.QuartersRepository;
import com.aivle.project.report.dto.ReportMetricRowDto;
import com.aivle.project.report.dto.ReportMetricGroupedResponse;
import com.aivle.project.report.entity.CompanyReportMetricValuesEntity;
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
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import({QuerydslConfig.class, CompanyReportMetricQueryService.class})
class CompanyReportMetricQueryServiceTest {

	@Autowired
	private CompanyReportMetricQueryService companyReportMetricQueryService;

	@Autowired
	private CompanyReportMetricValuesRepository companyReportMetricValuesRepository;

	@Autowired
	private CompanyReportVersionsRepository companyReportVersionsRepository;

	@Autowired
	private CompanyReportsRepository companyReportsRepository;

	@Autowired
	private CompaniesRepository companiesRepository;

	@Autowired
	private QuartersRepository quartersRepository;

	@Autowired
	private MetricsRepository metricsRepository;

	@org.springframework.boot.test.mock.mockito.MockBean
	private com.aivle.project.report.mapper.ReportMapper reportMapper;

	@Test
	@DisplayName("기업코드를 정규화하고 최신 버전 지표를 반환한다")
	void fetchLatestMetrics() {
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
		companyReportVersionsRepository.save(
			CompanyReportVersionsEntity.create(report, 1, LocalDateTime.now().minusDays(1), false, null)
		);
		CompanyReportVersionsEntity latestVersion = companyReportVersionsRepository.save(
			CompanyReportVersionsEntity.create(report, 2, LocalDateTime.now(), false, null)
		);

		companyReportMetricValuesRepository.save(CompanyReportMetricValuesEntity.create(
			latestVersion,
			metric,
			q20244,
			new BigDecimal("2.22"),
			MetricValueType.ACTUAL
		));
		companyReportMetricValuesRepository.save(CompanyReportMetricValuesEntity.create(
			latestVersion,
			metric,
			q20253,
			new BigDecimal("3.33"),
			MetricValueType.ACTUAL
		));

		org.mockito.BDDMockito.given(reportMapper.toRowDto(org.mockito.ArgumentMatchers.any())).willAnswer(invocation -> {
			com.aivle.project.report.dto.ReportMetricRowProjection p = invocation.getArgument(0);
			return new ReportMetricRowDto(
				p.getCorpName(), p.getStockCode(), p.getMetricCode(), p.getMetricNameKo(),
				p.getMetricValue(), p.getValueType(), p.getQuarterKey(), p.getVersionNo(), p.getGeneratedAt()
			);
		});

		// when
		List<ReportMetricRowDto> rows = companyReportMetricQueryService.fetchLatestMetrics("20", 20244, 20253);

		// then
		assertThat(rows).hasSize(2);
		assertThat(rows).allMatch(row -> row.versionNo() == 2);
		assertThat(rows).allMatch(row -> "000020".equals(row.stockCode()));
		assertThat(rows).extracting(ReportMetricRowDto::quarterKey)
			.containsExactly(20244, 20253);
	}

	@Test
	@DisplayName("최신 버전에 ACTUAL이 없으면 최신 ACTUAL 버전을 조회한다")
	void fetchLatestMetrics_fallbackToLatestActualVersion() {
		// given
		CompaniesEntity company = companiesRepository.save(CompaniesEntity.create(
			"00000003",
			"테스트기업3",
			"TEST_CO3",
			"000040",
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
		CompanyReportVersionsEntity actualVersion = companyReportVersionsRepository.save(
			CompanyReportVersionsEntity.create(report, 1, LocalDateTime.now().minusDays(1), false, null)
		);
		companyReportVersionsRepository.save(
			CompanyReportVersionsEntity.create(report, 2, LocalDateTime.now(), false, null)
		);

		companyReportMetricValuesRepository.save(CompanyReportMetricValuesEntity.create(
			actualVersion,
			metric,
			q20253,
			new BigDecimal("9.99"),
			MetricValueType.ACTUAL
		));

		org.mockito.BDDMockito.given(reportMapper.toRowDto(org.mockito.ArgumentMatchers.any())).willAnswer(invocation -> {
			com.aivle.project.report.dto.ReportMetricRowProjection p = invocation.getArgument(0);
			return new ReportMetricRowDto(
				p.getCorpName(), p.getStockCode(), p.getMetricCode(), p.getMetricNameKo(),
				p.getMetricValue(), p.getValueType(), p.getQuarterKey(), p.getVersionNo(), p.getGeneratedAt()
			);
		});

		// when
		List<ReportMetricRowDto> rows = companyReportMetricQueryService.fetchLatestMetrics("40", 20253, 20253);

		// then
		assertThat(rows).hasSize(1);
		assertThat(rows.get(0).versionNo()).isEqualTo(1);
		assertThat(rows.get(0).metricValue()).isEqualByComparingTo("9.99");
	}

	@Test
	@DisplayName("분기별로 그룹핑된 지표 응답을 반환한다")
	void fetchLatestMetricsGrouped() {
		// given
		CompaniesEntity company = companiesRepository.save(CompaniesEntity.create(
			"00000002",
			"테스트기업2",
			"TEST_CO2",
			"000030",
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
		MetricsEntity roa = metricsRepository.findByMetricCode("ROA").orElseThrow();
		MetricsEntity roe = metricsRepository.findByMetricCode("ROE").orElseThrow();

		CompanyReportsEntity report = companyReportsRepository.save(
			CompanyReportsEntity.create(company, q20253, null)
		);
		CompanyReportVersionsEntity latestVersion = companyReportVersionsRepository.save(
			CompanyReportVersionsEntity.create(report, 1, LocalDateTime.now(), false, null)
		);

		companyReportMetricValuesRepository.save(CompanyReportMetricValuesEntity.create(
			latestVersion,
			roa,
			q20244,
			new BigDecimal("1.11"),
			MetricValueType.ACTUAL
		));
		companyReportMetricValuesRepository.save(CompanyReportMetricValuesEntity.create(
			latestVersion,
			roe,
			q20244,
			new BigDecimal("2.22"),
			MetricValueType.ACTUAL
		));
		companyReportMetricValuesRepository.save(CompanyReportMetricValuesEntity.create(
			latestVersion,
			roa,
			q20253,
			new BigDecimal("3.33"),
			MetricValueType.ACTUAL
		));

		org.mockito.BDDMockito.given(reportMapper.toRowDto(org.mockito.ArgumentMatchers.any())).willAnswer(invocation -> {
			com.aivle.project.report.dto.ReportMetricRowProjection p = invocation.getArgument(0);
			return new ReportMetricRowDto(
				p.getCorpName(), p.getStockCode(), p.getMetricCode(), p.getMetricNameKo(),
				p.getMetricValue(), p.getValueType(), p.getQuarterKey(), p.getVersionNo(), p.getGeneratedAt()
			);
		});
		org.mockito.BDDMockito.given(reportMapper.toItemDto(org.mockito.ArgumentMatchers.any())).willAnswer(invocation -> {
			ReportMetricRowDto row = invocation.getArgument(0);
			return new com.aivle.project.report.dto.ReportMetricItemDto(
				row.metricCode(), row.metricNameKo(), row.metricValue(), row.valueType()
			);
		});
		org.mockito.BDDMockito.given(
			reportMapper.toGroupedResponse(
				org.mockito.ArgumentMatchers.anyString(),
				org.mockito.ArgumentMatchers.anyString(),
				org.mockito.ArgumentMatchers.anyInt(),
				org.mockito.ArgumentMatchers.anyInt(),
				org.mockito.ArgumentMatchers.anyList()
			)
		).willAnswer(invocation -> new ReportMetricGroupedResponse(
			invocation.getArgument(0),
			invocation.getArgument(1),
			invocation.getArgument(2),
			invocation.getArgument(3),
			invocation.getArgument(4)
		));

		// when
		ReportMetricGroupedResponse response = companyReportMetricQueryService
			.fetchLatestMetricsGrouped("30", 20244, 20253);

		// then
		assertThat(response.stockCode()).isEqualTo("000030");
		assertThat(response.quarters()).hasSize(2);
		assertThat(response.quarters().get(0).metrics()).hasSize(2);
		assertThat(response.quarters().get(1).metrics()).hasSize(1);
	}
}
