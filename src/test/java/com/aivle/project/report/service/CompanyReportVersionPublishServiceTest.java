package com.aivle.project.report.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.repository.CompaniesRepository;
import com.aivle.project.common.config.TestSecurityConfig;
import com.aivle.project.quarter.entity.QuartersEntity;
import com.aivle.project.quarter.repository.QuartersRepository;
import com.aivle.project.quarter.support.QuarterCalculator;
import com.aivle.project.quarter.support.YearQuarter;
import com.aivle.project.report.dto.ReportVersionPublishResult;
import com.aivle.project.report.entity.CompanyReportVersionsEntity;
import com.aivle.project.report.entity.CompanyReportsEntity;
import com.aivle.project.report.repository.CompanyReportVersionsRepository;
import com.aivle.project.report.repository.CompanyReportsRepository;
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
class CompanyReportVersionPublishServiceTest {

	@Autowired
	private CompanyReportVersionPublishService companyReportVersionPublishService;

	@Autowired
	private CompaniesRepository companiesRepository;

	@Autowired
	private QuartersRepository quartersRepository;

	@Autowired
	private CompanyReportsRepository companyReportsRepository;

	@Autowired
	private CompanyReportVersionsRepository companyReportVersionsRepository;

	@Test
	@DisplayName("최신 버전이 미발행이면 발행 상태로 전환한다")
	void publishLatestVersion_marksPublished() {
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
		CompanyReportVersionsEntity version = companyReportVersionsRepository.save(
			CompanyReportVersionsEntity.create(report, 1, LocalDateTime.now(), false, null)
		);

		// when
		ReportVersionPublishResult result = companyReportVersionPublishService.publishLatestVersion("000020", 20253);

		// then
		assertThat(result.published()).isEqualTo(1);
		assertThat(result.reportVersionNo()).isEqualTo(1);
		assertThat(companyReportVersionsRepository.findById(version.getId()).orElseThrow().isPublished()).isTrue();
	}

	@Test
	@DisplayName("이미 발행된 최신 버전은 그대로 유지한다")
	void publishLatestVersion_keepsPublishedVersion() {
		// given
		CompaniesEntity company = companiesRepository.save(CompaniesEntity.create(
			"00000002",
			"두번째기업",
			"SECOND_CO",
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
		CompanyReportVersionsEntity version = companyReportVersionsRepository.save(
			CompanyReportVersionsEntity.create(report, 1, LocalDateTime.now(), true, null)
		);

		// when
		ReportVersionPublishResult result = companyReportVersionPublishService.publishLatestVersion("000030", 20253);

		// then
		assertThat(result.published()).isEqualTo(0);
		assertThat(result.reportVersionNo()).isEqualTo(1);
		assertThat(companyReportVersionsRepository.findById(version.getId()).orElseThrow().isPublished()).isTrue();
	}
}
