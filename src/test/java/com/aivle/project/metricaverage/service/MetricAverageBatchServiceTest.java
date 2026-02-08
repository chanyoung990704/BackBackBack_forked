package com.aivle.project.metricaverage.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.aivle.project.common.config.QuerydslConfig;
import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.repository.CompaniesRepository;
import com.aivle.project.metric.entity.MetricValueType;
import com.aivle.project.metric.entity.MetricsEntity;
import com.aivle.project.metric.repository.MetricsRepository;
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
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import({QuerydslConfig.class, MetricAverageCalculationService.class, MetricAverageBatchService.class})
class MetricAverageBatchServiceTest {

	@Autowired
	private MetricAverageBatchService metricAverageBatchService;
	@Autowired
	private QuartersRepository quartersRepository;
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
	private MetricAverageRepository metricAverageRepository;

	@Test
	@DisplayName("저장된 모든 분기를 순회해 metric_averages를 저장한다")
	void calculateAndUpsertAllQuarters() {
		// given
		QuartersEntity q1 = quartersRepository.save(QuartersEntity.create(
			2025, 1, 20251, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 3, 31)
		));
		QuartersEntity q2 = quartersRepository.save(QuartersEntity.create(
			2025, 2, 20252, LocalDate.of(2025, 4, 1), LocalDate.of(2025, 6, 30)
		));

		MetricsEntity roe = metricsRepository.findByMetricCode("ROE").orElseThrow();
		CompaniesEntity company = companiesRepository.save(CompaniesEntity.create(
			"90000001", "배치기업", "BATCH", "900001", LocalDate.of(2025, 1, 1)
		));

		saveActualMetric(company, q1, roe, new BigDecimal("10"));
		saveActualMetric(company, q2, roe, new BigDecimal("20"));

		// when
		int processedQuarterCount = metricAverageBatchService.calculateAndUpsertAllQuarters();

		// then
		assertThat(processedQuarterCount).isGreaterThanOrEqualTo(2);
		List<MetricAverageEntity> averages = metricAverageRepository.findAll();
		assertThat(averages)
			.anySatisfy(a -> {
				assertThat(a.getQuarter().getId()).isEqualTo(q1.getId());
				assertThat(a.getMetric().getId()).isEqualTo(roe.getId());
				assertThat(a.getAvgValue()).isEqualByComparingTo("10.0000");
			})
			.anySatisfy(a -> {
				assertThat(a.getQuarter().getId()).isEqualTo(q2.getId());
				assertThat(a.getMetric().getId()).isEqualTo(roe.getId());
				assertThat(a.getAvgValue()).isEqualByComparingTo("20.0000");
			});
	}

	@Test
	@DisplayName("전체 분기 저장 시 기존 데이터는 skip한다")
	void calculateAndInsertMissingAllQuarters_skipExisting() {
		// given
		QuartersEntity q1 = quartersRepository.save(QuartersEntity.create(
			2026, 1, 20261, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31)
		));
		QuartersEntity q2 = quartersRepository.save(QuartersEntity.create(
			2026, 2, 20262, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 6, 30)
		));
		MetricsEntity roe = metricsRepository.findByMetricCode("ROE").orElseThrow();
		CompaniesEntity company = companiesRepository.save(CompaniesEntity.create(
			"90000002", "배치기업2", "BATCH2", "900002", LocalDate.of(2026, 1, 1)
		));
		saveActualMetric(company, q1, roe, new BigDecimal("15"));
		saveActualMetric(company, q2, roe, new BigDecimal("25"));

		metricAverageRepository.save(MetricAverageEntity.create(
			q1, roe,
			new BigDecimal("88.0000"), new BigDecimal("88.0000"), new BigDecimal("88.0000"),
			new BigDecimal("88.0000"), BigDecimal.ZERO.setScale(4), 1, LocalDateTime.now(), 1
		));

		// when
		MetricAverageBatchSaveResult result = metricAverageBatchService.calculateAndInsertMissingAllQuarters(
			"TEST",
			"exec-test"
		);

		// then
		assertThat(result.processedQuarterCount()).isGreaterThanOrEqualTo(2);
		assertThat(result.insertedCount()).isEqualTo(1);
		assertThat(result.skippedCount()).isEqualTo(1);
		assertThat(result.triggerType()).isEqualTo("TEST");
		assertThat(result.executionId()).isEqualTo("exec-test");
	}

	private void saveActualMetric(CompaniesEntity company, QuartersEntity quarter, MetricsEntity metric, BigDecimal value) {
		CompanyReportsEntity report = companyReportsRepository.save(CompanyReportsEntity.create(company, quarter, null));
		CompanyReportVersionsEntity version = companyReportVersionsRepository.save(
			CompanyReportVersionsEntity.create(report, 1, LocalDateTime.now(), true, null)
		);
		companyReportMetricValuesRepository.save(
			CompanyReportMetricValuesEntity.create(version, metric, quarter, value, MetricValueType.ACTUAL)
		);
	}
}
