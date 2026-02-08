package com.aivle.project.report.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.aivle.project.common.config.QuerydslConfig;
import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.repository.CompaniesRepository;
import com.aivle.project.metric.entity.MetricValueType;
import com.aivle.project.metric.entity.MetricsEntity;
import com.aivle.project.metric.repository.MetricsRepository;
import com.aivle.project.quarter.entity.QuartersEntity;
import com.aivle.project.quarter.repository.QuartersRepository;
import com.aivle.project.report.dto.ReportMetricRowProjection;
import com.aivle.project.report.entity.CompanyReportMetricValuesEntity;
import com.aivle.project.report.entity.CompanyReportVersionsEntity;
import com.aivle.project.report.entity.CompanyReportsEntity;
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
@Import(QuerydslConfig.class)
class CompanyReportMetricValuesRepositoryTest {

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

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("시드된 metrics 데이터는 isRiskIndicator 기본값 false를 가진다")
	void seededMetricHasDefaultRiskIndicatorFalse() {
		// given
		MetricsEntity metric = metricsRepository.findByMetricCode("ROA").orElseThrow();

		// when & then
		assertThat(metric.isRiskIndicator()).isFalse();
	}

	@Test
	@DisplayName("metrics 삽입 시 is_risk_indicator 미지정이면 DB 기본값 0이 저장된다")
	void insertMetricWithoutRiskIndicatorStoresDefaultZero() {
		// given
		jdbcTemplate.update(
			"INSERT INTO metrics (metric_code, metric_name_ko, metric_name_en, unit) VALUES (?, ?, ?, ?)",
			"TEST_RISK_DEFAULT",
			"테스트 리스크 기본값",
			"test_risk_default",
			"%"
		);

		// when
		MetricsEntity metric = metricsRepository.findByMetricCode("TEST_RISK_DEFAULT").orElseThrow();

		// then
		assertThat(metric.isRiskIndicator()).isFalse();
	}

	@Test
	@DisplayName("최신 버전 기준으로 분기 범위 지표를 조회한다")
	void findLatestMetricsByStockCodeAndQuarterRange() {
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
		CompanyReportVersionsEntity oldVersion = companyReportVersionsRepository.save(
			CompanyReportVersionsEntity.create(report, 1, LocalDateTime.now().minusDays(1), false, null)
		);
		CompanyReportVersionsEntity latestVersion = companyReportVersionsRepository.save(
			CompanyReportVersionsEntity.create(report, 2, LocalDateTime.now(), false, null)
		);

		companyReportMetricValuesRepository.save(CompanyReportMetricValuesEntity.create(
			oldVersion,
			metric,
			q20253,
			new BigDecimal("1.11"),
			MetricValueType.ACTUAL
		));
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

		// when
		List<ReportMetricRowProjection> rows = companyReportMetricValuesRepository
			.findLatestMetricsByStockCodeAndQuarterRange("000020", 20244, 20253);

		// then
		assertThat(rows).hasSize(2);
		assertThat(rows).allMatch(row -> row.getVersionNo() == 2);
		assertThat(rows).allMatch(row -> "테스트기업".equals(row.getCorpName()));
		assertThat(rows).allMatch(row -> "000020".equals(row.getStockCode()));
		assertThat(rows).allMatch(row -> "ROA".equals(row.getMetricCode()));
		assertThat(rows).extracting(ReportMetricRowProjection::getQuarterKey)
			.containsExactly(20244, 20253);
	}
}
