package com.aivle.project.metricaverage.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.repository.CompaniesRepository;
import com.aivle.project.metric.entity.MetricValueType;
import com.aivle.project.metric.entity.MetricsEntity;
import com.aivle.project.metric.repository.MetricsRepository;
import com.aivle.project.metricaverage.dto.MetricAverageResult;
import com.aivle.project.metricaverage.entity.MetricAverageEntity;
import com.aivle.project.metricaverage.repository.MetricAverageRepository;
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
@Import(MetricAverageCalculationService.class)
class MetricAverageCalculationServiceTest {

	@Autowired
	private MetricAverageCalculationService service;
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
	private MetricAverageRepository metricAverageRepository;
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("비위험 ACTUAL 최신 버전 값만 분기 집계한다")
	void calculateLatestNonRiskActualOnly() {
		// given
		QuartersEntity quarter = quartersRepository.save(QuartersEntity.create(2025, 3, 20253,
			LocalDate.of(2025, 7, 1), LocalDate.of(2025, 9, 30)));
		CompaniesEntity c1 = companiesRepository.save(CompaniesEntity.create("00010001", "A", "A", "100001", LocalDate.now()));
		CompaniesEntity c2 = companiesRepository.save(CompaniesEntity.create("00010002", "B", "B", "100002", LocalDate.now()));

		MetricsEntity roe = metricsRepository.findByMetricCode("ROE").orElseThrow();
		MetricsEntity roa = metricsRepository.findByMetricCode("ROA").orElseThrow();
		jdbcTemplate.update("UPDATE metrics SET is_risk_indicator = 1 WHERE id = ?", roa.getId());

		CompanyReportsEntity c1Report = companyReportsRepository.save(CompanyReportsEntity.create(c1, quarter, null));
		CompanyReportsEntity c2Report = companyReportsRepository.save(CompanyReportsEntity.create(c2, quarter, null));
		CompanyReportVersionsEntity c1v1 = createVersion(c1Report, 1);
		CompanyReportVersionsEntity c1v2 = createVersion(c1Report, 2);
		CompanyReportVersionsEntity c2v1 = createVersion(c2Report, 1);

		companyReportMetricValuesRepository.save(CompanyReportMetricValuesEntity.create(
			c1v1, roe, quarter, new BigDecimal("10"), MetricValueType.ACTUAL));
		companyReportMetricValuesRepository.save(CompanyReportMetricValuesEntity.create(
			c1v2, roe, quarter, new BigDecimal("30"), MetricValueType.ACTUAL));
		companyReportMetricValuesRepository.save(CompanyReportMetricValuesEntity.create(
			c2v1, roe, quarter, new BigDecimal("50"), MetricValueType.ACTUAL));
		companyReportMetricValuesRepository.save(CompanyReportMetricValuesEntity.create(
			c2v1, roe, quarter, new BigDecimal("999"), MetricValueType.PREDICTED));
		companyReportMetricValuesRepository.save(CompanyReportMetricValuesEntity.create(
			c2v1, roa, quarter, new BigDecimal("70"), MetricValueType.ACTUAL));

		// when
		List<MetricAverageResult> results = service.calculateAndUpsertByQuarter(quarter.getId());

		// then
		assertThat(results).hasSize(1);
		MetricAverageResult result = results.get(0);
		assertThat(result.metricId()).isEqualTo(roe.getId());
		assertThat(result.companyCount()).isEqualTo(2);
		assertThat(result.avgValue()).isEqualByComparingTo("40.0000");
		assertThat(result.medianValue()).isEqualByComparingTo("40.0000");
		assertThat(result.minValue()).isEqualByComparingTo("30.0000");
		assertThat(result.maxValue()).isEqualByComparingTo("50.0000");

		MetricAverageEntity saved = metricAverageRepository.findByQuarterIdAndMetricId(quarter.getId(), roe.getId()).orElseThrow();
		assertThat(saved.getCompanyCount()).isEqualTo(2);
		assertThat(saved.getAvgValue()).isEqualByComparingTo("40.0000");
	}

	private CompanyReportVersionsEntity createVersion(CompanyReportsEntity report, int versionNo) {
		return companyReportVersionsRepository.save(
			CompanyReportVersionsEntity.create(report, versionNo, LocalDateTime.now(), false, null)
		);
	}
}
