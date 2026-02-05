package com.aivle.project.watchlist.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.repository.CompaniesRepository;
import com.aivle.project.metric.entity.MetricValueType;
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
}
