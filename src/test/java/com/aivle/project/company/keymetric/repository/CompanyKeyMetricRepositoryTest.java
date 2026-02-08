package com.aivle.project.company.keymetric.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.aivle.project.common.config.QuerydslConfig;
import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.keymetric.entity.CompanyKeyMetricEntity;
import com.aivle.project.company.keymetric.entity.CompanyKeyMetricRiskLevel;
import com.aivle.project.company.repository.CompaniesRepository;
import com.aivle.project.quarter.entity.QuartersEntity;
import com.aivle.project.quarter.repository.QuartersRepository;
import com.aivle.project.report.entity.CompanyReportVersionsEntity;
import com.aivle.project.report.entity.CompanyReportsEntity;
import com.aivle.project.report.repository.CompanyReportVersionsRepository;
import com.aivle.project.report.repository.CompanyReportsRepository;
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
@Import(QuerydslConfig.class)
class CompanyKeyMetricRepositoryTest {

	@Autowired
	private CompanyKeyMetricRepository companyKeyMetricRepository;

	@Autowired
	private CompaniesRepository companiesRepository;

	@Autowired
	private QuartersRepository quartersRepository;

	@Autowired
	private CompanyReportsRepository companyReportsRepository;

	@Autowired
	private CompanyReportVersionsRepository companyReportVersionsRepository;

	@Test
	@DisplayName("기업/분기 기준으로 핵심 건강도를 조회한다")
	void findByCompanyAndQuarter() {
		// given
		CompaniesEntity company = companiesRepository.save(CompaniesEntity.create(
			"00000002",
			"테스트기업",
			"TEST_CO",
			"000021",
			LocalDate.of(2025, 2, 1)
		));
		QuartersEntity quarter = quartersRepository.save(QuartersEntity.create(
			2025,
			1,
			20251,
			LocalDate.of(2025, 1, 1),
			LocalDate.of(2025, 3, 31)
		));
		CompanyReportsEntity report = companyReportsRepository.save(
			CompanyReportsEntity.create(company, quarter, null)
		);
		CompanyReportVersionsEntity reportVersion = companyReportVersionsRepository.save(
			CompanyReportVersionsEntity.create(report, 1, LocalDateTime.now(), false, null)
		);
		CompanyKeyMetricEntity metric = CompanyKeyMetricEntity.create(
			company,
			quarter,
			reportVersion,
			new BigDecimal("70.00"),
			new BigDecimal("65.00"),
			new BigDecimal("68.00"),
			CompanyKeyMetricRiskLevel.SAFE,
			1,
			LocalDateTime.now()
		);
		companyKeyMetricRepository.save(metric);

		// when
		CompanyKeyMetricEntity found = companyKeyMetricRepository
			.findByCompanyIdAndQuarterId(company.getId(), quarter.getId())
			.orElseThrow();

		// then
		assertThat(found.getRiskLevel()).isEqualTo(CompanyKeyMetricRiskLevel.SAFE);
		assertThat(found.getCompositeScore()).isEqualTo(new BigDecimal("68.00"));
	}
}
