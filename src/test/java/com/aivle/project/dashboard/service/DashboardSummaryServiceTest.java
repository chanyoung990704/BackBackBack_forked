package com.aivle.project.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.aivle.project.common.config.QuerydslConfig;
import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.keymetric.entity.CompanyKeyMetricEntity;
import com.aivle.project.company.keymetric.entity.CompanyKeyMetricRiskLevel;
import com.aivle.project.company.keymetric.repository.CompanyKeyMetricRepository;
import com.aivle.project.company.repository.CompaniesRepository;
import com.aivle.project.dashboard.dto.CompanyQuarterRiskDto;
import com.aivle.project.dashboard.dto.DashboardSummaryResponse;
import com.aivle.project.dashboard.dto.KpiCardDto;
import com.aivle.project.industry.entity.IndustryEntity;
import com.aivle.project.industry.entity.IndustryRepository;
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
import com.aivle.project.user.entity.UserEntity;
import com.aivle.project.user.entity.UserStatus;
import com.aivle.project.user.repository.UserRepository;
import com.aivle.project.watchlist.entity.CompanyWatchlistEntity;
import com.aivle.project.watchlist.repository.CompanyWatchlistRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import({QuerydslConfig.class, DashboardSummaryService.class})
class DashboardSummaryServiceTest {

	@Autowired
	private DashboardSummaryService dashboardSummaryService;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private CompaniesRepository companiesRepository;
	@Autowired
	private IndustryRepository industryRepository;
	@Autowired
	private CompanyWatchlistRepository companyWatchlistRepository;
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
	private CompanyKeyMetricRepository companyKeyMetricRepository;

	@Test
	@DisplayName("대시보드 요약에서 FORECAST는 baseQuarter+1의 internal score/risk level 저장 분기를 사용한다")
	void getSummary_usesNextQuarterAsForecastFromKeyMetrics() {
		// given
		UserEntity user = userRepository.save(UserEntity.create("dash@test.com", "pw", "dash", null, UserStatus.ACTIVE));
		IndustryEntity foodIndustry = industryRepository.save(IndustryEntity.create("10000", "식품"));
		CompaniesEntity companyA = companiesRepository.save(
			CompaniesEntity.create("00001001", "기업A", "A", "100001", LocalDate.now(), foodIndustry)
		);
		CompaniesEntity companyB = companiesRepository.save(
			CompaniesEntity.create("00001002", "기업B", "B", "100002", LocalDate.now(), foodIndustry)
		);
		companyWatchlistRepository.save(CompanyWatchlistEntity.create(user, companyA, "A"));
		companyWatchlistRepository.save(CompanyWatchlistEntity.create(user, companyB, "B"));

		QuartersEntity q20244 = saveQuarter(2024, 4, 20244, LocalDate.of(2024, 10, 1), LocalDate.of(2024, 12, 31));
		QuartersEntity q20251 = saveQuarter(2025, 1, 20251, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 3, 31));
		QuartersEntity q20252 = saveQuarter(2025, 2, 20252, LocalDate.of(2025, 4, 1), LocalDate.of(2025, 6, 30));
		QuartersEntity q20253 = saveQuarter(2025, 3, 20253, LocalDate.of(2025, 7, 1), LocalDate.of(2025, 9, 30));
		QuartersEntity q20254 = saveQuarter(2025, 4, 20254, LocalDate.of(2025, 10, 1), LocalDate.of(2025, 12, 31));

		seedActualMetric(companyA, q20253, new BigDecimal("11.1"));
		seedActualMetric(companyB, q20253, new BigDecimal("22.2"));

		seedKeyMetric(companyA, q20244, CompanyKeyMetricRiskLevel.SAFE, 70);
		seedKeyMetric(companyB, q20244, CompanyKeyMetricRiskLevel.WARN, 45);
		seedKeyMetric(companyA, q20251, CompanyKeyMetricRiskLevel.WARN, 40);
		seedKeyMetric(companyB, q20251, CompanyKeyMetricRiskLevel.WARN, 42);
		seedKeyMetric(companyA, q20252, CompanyKeyMetricRiskLevel.RISK, 25);
		seedKeyMetric(companyB, q20252, CompanyKeyMetricRiskLevel.WARN, 55);
		seedKeyMetric(companyA, q20253, CompanyKeyMetricRiskLevel.WARN, 48);
		seedKeyMetric(companyB, q20253, CompanyKeyMetricRiskLevel.RISK, 30);
		// forecast quarter(base+1)
		seedKeyMetric(companyA, q20254, CompanyKeyMetricRiskLevel.SAFE, 72);
		seedKeyMetric(companyB, q20254, CompanyKeyMetricRiskLevel.WARN, 44);

		// when
		DashboardSummaryResponse response = dashboardSummaryService.getSummary(user.getId());

		// then
		assertThat(response.latestActualQuarter()).isEqualTo("2025Q3");
		assertThat(response.forecastQuarter()).isEqualTo("2025Q4");
		assertThat(response.windowQuarters()).containsExactly("2024Q4", "2025Q1", "2025Q2", "2025Q3", "2025Q4");
		assertThat(response.riskStatusDistribution().NORMAL()).isEqualTo(0);
		assertThat(response.riskStatusDistribution().CAUTION()).isEqualTo(1);
		assertThat(response.riskStatusDistribution().RISK()).isEqualTo(1);
		assertThat(response.riskStatusDistributionTrend()).hasSize(5);
		assertThat(response.riskStatusDistributionTrend().get(4).dataType().name()).isEqualTo("FORECAST");
		assertThat(response.riskStatusDistributionTrend().get(4).NORMAL()).isEqualTo(1);
		assertThat(response.riskStatusDistributionTrend().get(4).CAUTION()).isEqualTo(1);
		assertThat(response.riskStatusDistributionTrend().get(4).RISK()).isEqualTo(0);
		assertThat(response.kpis()).hasSize(6);
		Map<String, KpiCardDto> kpiByKey = response.kpis().stream()
			.collect(Collectors.toMap(KpiCardDto::key, Function.identity()));

		assertThat(kpiByKey.get("NETWORK_STATUS").value()).isEqualTo(39.0);
		assertThat(kpiByKey.get("NETWORK_STATUS").unit()).isEqualTo("%");
		assertThat(kpiByKey.get("RISK_INDEX").value()).isEqualTo(75.0);
		assertThat(kpiByKey.get("RISK_INDEX").unit()).isEqualTo("점");

		assertThat(kpiByKey.get("RISK_DWELL_TIME").value()).isEqualTo(3.5);
		assertThat(kpiByKey.get("RISK_DWELL_TIME").unit()).isEqualTo("분기");
		assertThat(kpiByKey.get("RISK_DWELL_TIME").delta()).isNotNull();
		assertThat(kpiByKey.get("RISK_DWELL_TIME").delta().value()).isEqualTo(1.0);
		assertThat(kpiByKey.get("RISK_DWELL_TIME").delta().direction().name()).isEqualTo("UP");
		assertThat(kpiByKey.get("RISK_DWELL_TIME").delta().label()).isEqualTo("지난 분기 대비");

		assertThat(response.riskStatusDistributionPercent().NORMAL()).isEqualTo(0.0);
		assertThat(response.riskStatusDistributionPercent().CAUTION()).isEqualTo(50.0);
		assertThat(response.riskStatusDistributionPercent().RISK()).isEqualTo(50.0);
		assertThat(response.averageRiskLevel()).isEqualTo(61.0);
		assertThat(response.majorSector()).isNotNull();
		assertThat(response.majorSector().name()).isEqualTo("식품");
		assertThat(response.majorSector().riskCompanyCount()).isEqualTo(2);
		assertThat(response.majorSector().totalCompanyCount()).isEqualTo(2);
		assertThat(response.majorSector().riskRatio()).isEqualTo(100.0);
		assertThat(response.majorSector().riskIndex()).isEqualTo(75.0);
	}

	@Test
	@DisplayName("리스크 레코드는 워치리스트 기준 ACTUAL만 limit 만큼 최신 분기순으로 반환한다")
	void getRiskRecords_returnsActualOnlyWithLimit() {
		// given
		UserEntity user = userRepository.save(UserEntity.create("risk@test.com", "pw", "risk", null, UserStatus.ACTIVE));
		IndustryEntity industry = industryRepository.save(IndustryEntity.create("20000", "제조"));
		CompaniesEntity companyA = companiesRepository.save(
			CompaniesEntity.create("00002001", "기업A", "A", "200001", LocalDate.now(), industry)
		);
		CompaniesEntity companyB = companiesRepository.save(
			CompaniesEntity.create("00002002", "기업B", "B", "200002", LocalDate.now(), industry)
		);
		companyWatchlistRepository.save(CompanyWatchlistEntity.create(user, companyA, "A"));
		companyWatchlistRepository.save(CompanyWatchlistEntity.create(user, companyB, "B"));

		QuartersEntity q20253 = saveQuarter(2025, 3, 20253, LocalDate.of(2025, 7, 1), LocalDate.of(2025, 9, 30));
		QuartersEntity q20254 = saveQuarter(2025, 4, 20254, LocalDate.of(2025, 10, 1), LocalDate.of(2025, 12, 31));

		seedActualMetric(companyA, q20253, new BigDecimal("33.0"));
		seedActualMetric(companyB, q20253, new BigDecimal("44.0"));

		seedKeyMetric(companyA, q20253, CompanyKeyMetricRiskLevel.SAFE, 65);
		seedKeyMetric(companyB, q20253, CompanyKeyMetricRiskLevel.WARN, 45);
		seedKeyMetric(companyA, q20254, CompanyKeyMetricRiskLevel.RISK, 20);
		seedKeyMetric(companyB, q20254, CompanyKeyMetricRiskLevel.RISK, 25);

		// when
		List<CompanyQuarterRiskDto> records = dashboardSummaryService.getRiskRecords(user.getId(), 2);

		// then
		assertThat(records).hasSize(2);
		assertThat(records).extracting(CompanyQuarterRiskDto::quarter).containsOnly("2025Q3");
		assertThat(records).extracting(CompanyQuarterRiskDto::riskLevel)
			.containsExactlyInAnyOrder(CompanyQuarterRiskDto.RiskLevel.MIN, CompanyQuarterRiskDto.RiskLevel.WARN);
	}

	private QuartersEntity saveQuarter(int year, int quarter, int quarterKey, LocalDate start, LocalDate end) {
		return quartersRepository.save(QuartersEntity.create(year, quarter, quarterKey, start, end));
	}

	private void seedActualMetric(CompaniesEntity company, QuartersEntity quarter, BigDecimal value) {
		CompanyReportsEntity report = companyReportsRepository.save(CompanyReportsEntity.create(company, quarter, null));
		CompanyReportVersionsEntity version = companyReportVersionsRepository.save(
			CompanyReportVersionsEntity.create(report, 1, LocalDateTime.now(), true, null)
		);
		MetricsEntity roe = metricsRepository.findByMetricCode("ROE").orElseThrow();
		companyReportMetricValuesRepository.save(
			CompanyReportMetricValuesEntity.create(version, roe, quarter, value, MetricValueType.ACTUAL)
		);
	}

	private void seedKeyMetric(
		CompaniesEntity company,
		QuartersEntity quarter,
		CompanyKeyMetricRiskLevel riskLevel,
		double score
	) {
		companyKeyMetricRepository.save(CompanyKeyMetricEntity.create(
			company,
			quarter,
			null,
			BigDecimal.valueOf(score),
			null,
			BigDecimal.valueOf(score),
			riskLevel,
			1,
			LocalDateTime.now()
		));
	}
}
