package com.aivle.project.company.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.aivle.project.common.config.QuerydslConfig;
import com.aivle.project.company.dto.CompanyInfoDto;
import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.keymetric.entity.CompanyKeyMetricEntity;
import com.aivle.project.company.keymetric.entity.CompanyKeyMetricRiskLevel;
import com.aivle.project.company.keymetric.repository.CompanyKeyMetricRepository;
import com.aivle.project.company.news.entity.NewsAnalysisEntity;
import com.aivle.project.company.news.repository.NewsAnalysisRepository;
import com.aivle.project.company.news.service.NewsService;
import com.aivle.project.company.repository.CompaniesRepository;
import com.aivle.project.industry.entity.IndustryEntity;
import com.aivle.project.industry.entity.IndustryRepository;
import com.aivle.project.quarter.entity.QuartersEntity;
import com.aivle.project.quarter.repository.QuartersRepository;
import com.aivle.project.quarter.support.QuarterCalculator;
import com.aivle.project.quarter.support.YearQuarter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import({
	QuerydslConfig.class,
	CompanyInfoService.class,
	CompanySectorService.class,
	CompanyReputationScoreService.class
})
class CompanyInfoServiceTest {

	@Autowired
	private CompanyInfoService companyInfoService;

	@Autowired
	private CompaniesRepository companiesRepository;

	@Autowired
	private IndustryRepository industryRepository;

	@Autowired
	private QuartersRepository quartersRepository;

	@Autowired
	private CompanyKeyMetricRepository companyKeyMetricRepository;

	@Autowired
	private NewsAnalysisRepository newsAnalysisRepository;

	@MockBean
	private NewsService newsService;

	@Test
	@DisplayName("기업/분기 기준으로 기본 정보를 구성한다")
	void getCompanyInfo() {
		// given
		IndustryEntity industry = industryRepository.save(IndustryEntity.create("A0101", "식품"));
		CompaniesEntity company = companiesRepository.save(CompaniesEntity.create(
			"00000001",
			"테스트기업",
			"TEST_CO",
			"000020",
			LocalDate.of(2025, 1, 1),
			industry
		));

		int quarterKey = 20251;
		YearQuarter yearQuarter = QuarterCalculator.parseQuarterKey(quarterKey);
		QuartersEntity quarter = quartersRepository.save(QuartersEntity.create(
			yearQuarter.year(),
			yearQuarter.quarter(),
			quarterKey,
			QuarterCalculator.startDate(yearQuarter),
			QuarterCalculator.endDate(yearQuarter)
		));

		companyKeyMetricRepository.save(CompanyKeyMetricEntity.create(
			company,
			quarter,
			null,
			BigDecimal.valueOf(82.5),
			BigDecimal.valueOf(61.2),
			BigDecimal.valueOf(70.1),
			CompanyKeyMetricRiskLevel.SAFE,
			1,
			LocalDateTime.now()
		));

		// when
		CompanyInfoDto result = companyInfoService.getCompanyInfo(company.getId(), String.valueOf(quarterKey));

		// then
		assertThat(result.getId()).isEqualTo(company.getId());
		assertThat(result.getName()).isEqualTo("테스트기업");
		assertThat(result.getStockCode()).isEqualTo("000020");
		assertThat(result.getSector().getLabel()).isEqualTo("식품");
		assertThat(result.getNetworkHealth()).isEqualTo(82.5);
		assertThat(result.getOverallScore()).isEqualTo(70.1);
		assertThat(result.getRiskLevel()).isEqualTo("SAFE");
		assertThat(result.getReputationScore()).isEqualTo(6120.0);
	}

	@Test
	@DisplayName("핵심 건강도 정보가 없으면 점수는 null로 반환한다")
	void getCompanyInfoWhenKeyMetricMissing() {
		// given
		IndustryEntity industry = industryRepository.save(IndustryEntity.create("B0202", "화학"));
		CompaniesEntity company = companiesRepository.save(CompaniesEntity.create(
			"00000002",
			"테스트기업2",
			"TEST_CO2",
			"000030",
			LocalDate.of(2025, 1, 2),
			industry
		));

		int quarterKey = 20252;
		YearQuarter yearQuarter = QuarterCalculator.parseQuarterKey(quarterKey);
		quartersRepository.save(QuartersEntity.create(
			yearQuarter.year(),
			yearQuarter.quarter(),
			quarterKey,
			QuarterCalculator.startDate(yearQuarter),
			QuarterCalculator.endDate(yearQuarter)
		));

		// when
		CompanyInfoDto result = companyInfoService.getCompanyInfo(company.getId(), String.valueOf(quarterKey));

		// then
		assertThat(result.getNetworkHealth()).isNull();
		assertThat(result.getOverallScore()).isNull();
		assertThat(result.getRiskLevel()).isNull();
		assertThat(result.getReputationScore()).isNull();
	}

	@Test
	@DisplayName("외부 건강도 점수가 없으면 뉴스 평균 점수로 대체한다")
	void getCompanyInfoFallbackToNewsAverageScore() {
		// given
		IndustryEntity industry = industryRepository.save(IndustryEntity.create("D0404", "IT"));
		CompaniesEntity company = companiesRepository.save(CompaniesEntity.create(
			"00000004",
			"테스트기업4",
			"TEST_CO4",
			"000050",
			LocalDate.of(2025, 1, 4),
			industry
		));

		int quarterKey = 20253;
		YearQuarter yearQuarter = QuarterCalculator.parseQuarterKey(quarterKey);
		QuartersEntity quarter = quartersRepository.save(QuartersEntity.create(
			yearQuarter.year(),
			yearQuarter.quarter(),
			quarterKey,
			QuarterCalculator.startDate(yearQuarter),
			QuarterCalculator.endDate(yearQuarter)
		));

		companyKeyMetricRepository.save(CompanyKeyMetricEntity.create(
			company,
			quarter,
			null,
			BigDecimal.valueOf(80.0),
			null,
			BigDecimal.valueOf(75.0),
			CompanyKeyMetricRiskLevel.SAFE,
			1,
			LocalDateTime.now()
		));

		newsAnalysisRepository.save(NewsAnalysisEntity.create(
			company,
			company.getCorpName(),
			10,
			BigDecimal.valueOf(64.25),
			LocalDateTime.now()
		));

		// when
		CompanyInfoDto result = companyInfoService.getCompanyInfo(company.getId(), String.valueOf(quarterKey));

		// then
		assertThat(result.getReputationScore()).isEqualTo(6425.0);
	}

	@Test
	@DisplayName("6자리 분기키 입력은 5자리 분기키로 정규화한다")
	void getCompanyInfoWithSixDigitQuarterKey() {
		// given
		IndustryEntity industry = industryRepository.save(IndustryEntity.create("C0303", "금융"));
		CompaniesEntity company = companiesRepository.save(CompaniesEntity.create(
			"00000003",
			"테스트기업3",
			"TEST_CO3",
			"000040",
			LocalDate.of(2025, 1, 3),
			industry
		));

		int normalizedQuarterKey = 20243;
		YearQuarter yearQuarter = QuarterCalculator.parseQuarterKey(normalizedQuarterKey);
		quartersRepository.save(QuartersEntity.create(
			yearQuarter.year(),
			yearQuarter.quarter(),
			normalizedQuarterKey,
			QuarterCalculator.startDate(yearQuarter),
			QuarterCalculator.endDate(yearQuarter)
		));

		// when
		CompanyInfoDto result = companyInfoService.getCompanyInfo(company.getId(), "202403");

		// then
		assertThat(result.getId()).isEqualTo(company.getId());
		assertThat(result.getSector().getLabel()).isEqualTo("금융");
	}
}
