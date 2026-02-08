package com.aivle.project.company.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aivle.project.common.config.QuerydslConfig;
import com.aivle.project.company.dto.CompanySectorDto;
import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.repository.CompaniesRepository;
import com.aivle.project.industry.entity.IndustryEntity;
import com.aivle.project.industry.entity.IndustryRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import({QuerydslConfig.class, CompanySectorService.class})
class CompanySectorServiceTest {

	@Autowired
	private CompanySectorService companySectorService;

	@Autowired
	private CompaniesRepository companiesRepository;

	@Autowired
	private IndustryRepository industryRepository;

	@Test
	@DisplayName("기업 업종명이 있으면 섹터 라벨로 반환한다")
	void getSectorReturnsIndustryName() {
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

		// when
		CompanySectorDto result = companySectorService.getSector(company.getId());

		// then
		assertThat(result.getKey()).isEmpty();
		assertThat(result.getLabel()).isEqualTo("식품");
	}

	@Test
	@DisplayName("업종이 없으면 빈 라벨을 반환한다")
	void getSectorReturnsEmptyLabelWhenIndustryMissing() {
		// given
		CompaniesEntity company = companiesRepository.save(CompaniesEntity.create(
			"00000002",
			"테스트기업2",
			"TEST_CO2",
			"000030",
			LocalDate.of(2025, 1, 1)
		));

		// when
		CompanySectorDto result = companySectorService.getSector(company.getId());

		// then
		assertThat(result.getKey()).isEmpty();
		assertThat(result.getLabel()).isEmpty();
	}

	@Test
	@DisplayName("기업이 없으면 예외를 던진다")
	void getSectorThrowsWhenCompanyMissing() {
		// given
		Long missingCompanyId = 9999L;

		// when & then
		assertThatThrownBy(() -> companySectorService.getSector(missingCompanyId))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Company not found");
	}
}
