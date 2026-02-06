package com.aivle.project.watchlist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.repository.CompaniesRepository;
import com.aivle.project.metric.entity.MetricValueType;
import com.aivle.project.common.error.CommonException;
import com.aivle.project.metric.entity.MetricsEntity;
import com.aivle.project.metric.repository.MetricsRepository;
import com.aivle.project.metricaverage.repository.MetricAverageRepository;
import com.aivle.project.quarter.entity.QuartersEntity;
import com.aivle.project.quarter.repository.QuartersRepository;
import com.aivle.project.report.entity.CompanyReportMetricValuesEntity;
import com.aivle.project.report.entity.CompanyReportVersionsEntity;
import com.aivle.project.report.entity.CompanyReportsEntity;
import com.aivle.project.report.repository.CompanyReportMetricValuesRepository;
import com.aivle.project.report.repository.CompanyReportVersionsRepository;
import com.aivle.project.report.repository.CompanyReportsRepository;
import com.aivle.project.risk.entity.RiskLevel;
import com.aivle.project.risk.entity.RiskScoreSummaryEntity;
import com.aivle.project.risk.repository.RiskScoreSummaryRepository;
import com.aivle.project.user.entity.RoleEntity;
import com.aivle.project.user.entity.RoleName;
import com.aivle.project.user.entity.UserEntity;
import com.aivle.project.user.entity.UserRoleEntity;
import com.aivle.project.user.entity.UserStatus;
import com.aivle.project.user.repository.RoleRepository;
import com.aivle.project.user.repository.UserRepository;
import com.aivle.project.user.repository.UserRoleRepository;
import com.aivle.project.watchlist.dto.WatchlistDashboardResponse;
import com.aivle.project.watchlist.dto.WatchlistMetricAveragesResponse;
import com.aivle.project.watchlist.error.WatchlistErrorCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(CompanyWatchlistService.class)
class CompanyWatchlistServiceTest {
	@Autowired CompanyWatchlistService service;
	@Autowired UserRepository userRepository;
	@Autowired RoleRepository roleRepository;
	@Autowired UserRoleRepository userRoleRepository;
	@Autowired CompaniesRepository companiesRepository;
	@Autowired QuartersRepository quartersRepository;
	@Autowired CompanyReportsRepository companyReportsRepository;
	@Autowired CompanyReportVersionsRepository companyReportVersionsRepository;
	@Autowired CompanyReportMetricValuesRepository metricValuesRepository;
	@Autowired MetricsRepository metricsRepository;
	@Autowired RiskScoreSummaryRepository riskScoreSummaryRepository;

	@Test
	@DisplayName("동일 기업을 중복 저장하면 CONFLICT 예외를 반환한다")
	void addWatchlistThrowsWhenDuplicate() {
		// given
		UserEntity user = userRepository.save(UserEntity.create("dup@test.com", "pw", "dup", null, UserStatus.ACTIVE));
		RoleEntity role = roleRepository.save(new RoleEntity(RoleName.ROLE_USER, "user"));
		userRoleRepository.save(new UserRoleEntity(user, role));
		CompaniesEntity company = companiesRepository.save(CompaniesEntity.create("00000078", "중복기업", "DUP", "778888", LocalDate.now()));
		service.addWatchlist(user.getId(), company.getId(), "first");

		// when & then
		assertThatThrownBy(() -> service.addWatchlist(user.getId(), company.getId(), "second"))
			.isInstanceOf(CommonException.class)
			.extracting(ex -> ((CommonException) ex).getErrorCode())
			.isEqualTo(WatchlistErrorCode.WATCHLIST_DUPLICATE);
	}

	@Test
	@DisplayName("watchlist 대시보드는 최신 버전 ACTUAL 비위험 지표만 조회한다")
	void dashboardFiltersCorrectly() {
		// given
		UserEntity user = userRepository.save(UserEntity.create("w@test.com", "pw", "w", null, UserStatus.ACTIVE));
		RoleEntity role = roleRepository.save(new RoleEntity(RoleName.ROLE_USER, "user"));
		userRoleRepository.save(new UserRoleEntity(user, role));
		CompaniesEntity company = companiesRepository.save(CompaniesEntity.create("00000077", "기업", "C", "777777", LocalDate.now()));
		service.addWatchlist(user.getId(), company.getId(), "note");

		QuartersEntity q = quartersRepository.save(QuartersEntity.create(2024, 2, 20242, LocalDate.of(2024,4,1), LocalDate.of(2024,6,30)));
		CompanyReportsEntity report = companyReportsRepository.save(CompanyReportsEntity.create(company, q, null));
		CompanyReportVersionsEntity v1 = companyReportVersionsRepository.save(CompanyReportVersionsEntity.create(report, 1, LocalDateTime.now().minusDays(1), true, null));
		CompanyReportVersionsEntity v2 = companyReportVersionsRepository.save(CompanyReportVersionsEntity.create(report, 2, LocalDateTime.now(), true, null));
		MetricsEntity roe = metricsRepository.findByMetricCode("ROE").orElseThrow();
		MetricsEntity roa = metricsRepository.findByMetricCode("ROA").orElseThrow();
		roa.getClass();
		metricValuesRepository.save(CompanyReportMetricValuesEntity.create(v1, roe, q, new BigDecimal("10"), MetricValueType.ACTUAL));
		metricValuesRepository.save(CompanyReportMetricValuesEntity.create(v2, roe, q, new BigDecimal("20"), MetricValueType.ACTUAL));
		metricValuesRepository.save(CompanyReportMetricValuesEntity.create(v2, roe, q, new BigDecimal("50"), MetricValueType.PREDICTED));
		riskScoreSummaryRepository.save(RiskScoreSummaryEntity.create(company, q, v2, new BigDecimal("70"), RiskLevel.DANGER, 2, new BigDecimal("70"), LocalDateTime.now()));

		// when
		WatchlistDashboardResponse response = service.getDashboard(user.getId(), 2024, 2, null, null);

		// then
		assertThat(response.metrics()).hasSize(1);
		assertThat(response.metrics().get(0).metricValue()).isEqualByComparingTo("20.0000");
		assertThat(response.risks()).hasSize(1);
		assertThat(response.risks().get(0).riskLevel()).isEqualTo(RiskLevel.DANGER);
	}

	@Test
	@DisplayName("watchlist 지표 평균은 비위험 ACTUAL 최신 발행 버전 기준으로 계산한다")
	void metricAveragesFiltersCorrectly() {
		// given
		UserEntity user = userRepository.save(UserEntity.create("avg@test.com", "pw", "avg", null, UserStatus.ACTIVE));
		RoleEntity role = roleRepository.save(new RoleEntity(RoleName.ROLE_USER, "user"));
		userRoleRepository.save(new UserRoleEntity(user, role));

		CompaniesEntity companyA = companiesRepository.save(CompaniesEntity.create("00000011", "기업A", "A", "111111", LocalDate.now()));
		CompaniesEntity companyB = companiesRepository.save(CompaniesEntity.create("00000022", "기업B", "B", "222222", LocalDate.now()));
		service.addWatchlist(user.getId(), companyA.getId(), "A");
		service.addWatchlist(user.getId(), companyB.getId(), "B");

		QuartersEntity q = quartersRepository.save(QuartersEntity.create(2025, 3, 20253, LocalDate.of(2025, 7, 1), LocalDate.of(2025, 9, 30)));
		CompanyReportsEntity reportA = companyReportsRepository.save(CompanyReportsEntity.create(companyA, q, null));
		CompanyReportsEntity reportB = companyReportsRepository.save(CompanyReportsEntity.create(companyB, q, null));
		CompanyReportVersionsEntity aV1 = companyReportVersionsRepository.save(CompanyReportVersionsEntity.create(reportA, 1, LocalDateTime.now().minusDays(1), true, null));
		CompanyReportVersionsEntity aV2 = companyReportVersionsRepository.save(CompanyReportVersionsEntity.create(reportA, 2, LocalDateTime.now(), true, null));
		CompanyReportVersionsEntity bV1 = companyReportVersionsRepository.save(CompanyReportVersionsEntity.create(reportB, 1, LocalDateTime.now(), true, null));

		MetricsEntity roe = metricsRepository.findByMetricCode("ROE").orElseThrow();
		MetricsEntity riskMetric = metricsRepository.findByMetricCode("ROA").orElseThrow();
		metricValuesRepository.save(CompanyReportMetricValuesEntity.create(aV1, roe, q, new BigDecimal("10"), MetricValueType.ACTUAL));
		metricValuesRepository.save(CompanyReportMetricValuesEntity.create(aV2, roe, q, new BigDecimal("20"), MetricValueType.ACTUAL));
		metricValuesRepository.save(CompanyReportMetricValuesEntity.create(bV1, roe, q, new BigDecimal("40"), MetricValueType.ACTUAL));
		metricValuesRepository.save(CompanyReportMetricValuesEntity.create(bV1, roe, q, new BigDecimal("90"), MetricValueType.PREDICTED));
		riskMetric.getClass();

		// when
		WatchlistMetricAveragesResponse response = service.getWatchlistMetricAverages(user.getId(), 2025, 3, null);

		// then
		assertThat(response.metrics()).hasSize(1);
		assertThat(response.metrics().get(0).metricCode()).isEqualTo("ROE");
		assertThat(response.metrics().get(0).avgValue()).isEqualByComparingTo("30.0000");
		assertThat(response.metrics().get(0).sampleCompanyCount()).isEqualTo(2L);
	}
}
