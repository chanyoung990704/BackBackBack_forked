package com.aivle.project.company.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import com.aivle.project.common.config.QuerydslConfig;
import com.aivle.project.company.client.AiServerClient;
import com.aivle.project.company.dto.AiHealthScoreResponse;
import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.keymetric.entity.CompanyKeyMetricEntity;
import com.aivle.project.company.keymetric.entity.CompanyKeyMetricRiskLevel;
import com.aivle.project.company.keymetric.repository.CompanyKeyMetricRepository;
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
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import({QuerydslConfig.class, CompanyHealthScoreCacheService.class})
class CompanyHealthScoreCacheServiceTest {

	@Autowired
	private CompanyHealthScoreCacheService companyHealthScoreCacheService;

	@Autowired
	private CompaniesRepository companiesRepository;

	@Autowired
	private IndustryRepository industryRepository;

	@Autowired
	private QuartersRepository quartersRepository;

	@Autowired
	private CompanyKeyMetricRepository companyKeyMetricRepository;

	@MockBean
	private AiServerClient aiServerClient;

	@Test
	@DisplayName("재무건전성 점수가 없으면 AI 응답을 캐시하고 저장한다")
	void ensureHealthScoreCached() {
		// given
		IndustryEntity industry = industryRepository.save(IndustryEntity.create("A0101", "식품"));
		CompaniesEntity company = companiesRepository.save(CompaniesEntity.create(
			"00000001",
			"테스트기업",
			"TEST_CO",
			"005930",
			LocalDate.of(2025, 1, 1),
			industry
		));

		given(aiServerClient.getHealthScore(company.getStockCode()))
			.willReturn(new AiHealthScoreResponse(
				company.getStockCode(),
				company.getCorpName(),
				List.of(
					new AiHealthScoreResponse.HealthScoreQuarter("20253", 90.0, "안정", "actual"),
					new AiHealthScoreResponse.HealthScoreQuarter("20254", 80.0, "주의", "predicted")
				),
				90,
				80
			));

		// when
		companyHealthScoreCacheService.ensureHealthScoreCached(company.getId(), 20253);

		// then
		CompanyKeyMetricEntity actual = companyKeyMetricRepository
			.findByCompanyIdAndQuarter_QuarterKey(company.getId(), 20253)
			.orElseThrow();
		CompanyKeyMetricEntity predicted = companyKeyMetricRepository
			.findByCompanyIdAndQuarter_QuarterKey(company.getId(), 20254)
			.orElseThrow();

		assertThat(actual.getInternalHealthScore()).isEqualByComparingTo(BigDecimal.valueOf(90.0));
		assertThat(actual.getRiskLevel()).isEqualTo(CompanyKeyMetricRiskLevel.SAFE);
		assertThat(predicted.getInternalHealthScore()).isEqualByComparingTo(BigDecimal.valueOf(80.0));
		assertThat(predicted.getRiskLevel()).isEqualTo(CompanyKeyMetricRiskLevel.WARN);
	}

	@Test
	@DisplayName("이미 점수가 저장되어 있으면 AI 호출을 생략한다")
	void skipWhenCached() {
		// given
		IndustryEntity industry = industryRepository.save(IndustryEntity.create("A0101", "식품"));
		CompaniesEntity company = companiesRepository.save(CompaniesEntity.create(
			"00000002",
			"테스트기업2",
			"TEST_CO2",
			"000020",
			LocalDate.of(2025, 1, 1),
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
			BigDecimal.valueOf(88.0),
			null,
			BigDecimal.valueOf(88.0),
			CompanyKeyMetricRiskLevel.SAFE,
			1,
			LocalDateTime.now()
		));

		// when
		companyHealthScoreCacheService.ensureHealthScoreCached(company.getId(), quarterKey);

		// then
		verifyNoInteractions(aiServerClient);
	}

	@Test
	@DisplayName("요청 분기 엔티티가 없고 AI 응답이 비어있으면 fallback 엔티티를 생성한다")
	void getOrCreateKeyMetricFallback() {
		// given
		IndustryEntity industry = industryRepository.save(IndustryEntity.create("A0103", "서비스"));
		CompaniesEntity company = companiesRepository.save(CompaniesEntity.create(
			"00000003",
			"테스트기업3",
			"TEST_CO3",
			"123456",
			LocalDate.of(2025, 1, 1),
			industry
		));
		int quarterKey = 20252;

		given(aiServerClient.getHealthScore(company.getStockCode()))
			.willReturn(new AiHealthScoreResponse(
				company.getStockCode(),
				company.getCorpName(),
				List.of(),
				0,
				0
			));

		// when
		CompanyKeyMetricEntity entity = companyHealthScoreCacheService.getOrCreateKeyMetric(company.getId(), quarterKey);

		// then
		assertThat(entity.getQuarter().getQuarterKey()).isEqualTo(quarterKey);
		assertThat(entity.getRiskLevel()).isEqualTo(CompanyKeyMetricRiskLevel.WARN);
	}
}
