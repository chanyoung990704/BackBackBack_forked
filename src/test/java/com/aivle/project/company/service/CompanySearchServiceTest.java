package com.aivle.project.company.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.aivle.project.common.config.QuerydslConfig;
import com.aivle.project.company.dto.CompanySearchResponse;
import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.repository.CompaniesRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import({QuerydslConfig.class, CompanySearchService.class})
class CompanySearchServiceTest {

	@Autowired
	private CompanySearchService companySearchService;

	@Autowired
	private CompaniesRepository companiesRepository;

	@org.springframework.boot.test.mock.mockito.MockBean
	private com.aivle.project.company.mapper.CompanyMapper companyMapper;

	@Test
	@DisplayName("회사명 또는 영문명으로 기업을 검색한다")
	void searchCompanies() {
		// given
		CompaniesEntity company = companiesRepository.save(CompaniesEntity.create(
			"00000001",
			"테스트기업",
			"TEST_CO",
			"000020",
			LocalDate.of(2025, 1, 1)
		));
		companiesRepository.save(CompaniesEntity.create(
			"00000002",
			"다른회사",
			"OTHER_CO",
			"000030",
			LocalDate.of(2025, 1, 1)
		));
		companiesRepository.save(CompaniesEntity.create(
			"00000003",
			"테스트홀딩스",
			"TEST_HOLDINGS",
			null,
			LocalDate.of(2025, 1, 1)
		));

		org.mockito.BDDMockito.given(companyMapper.toSearchResponse(org.mockito.ArgumentMatchers.any())).willAnswer(invocation -> {
			CompaniesEntity c = invocation.getArgument(0);
			return new CompanySearchResponse(c.getId(), c.getCorpName(), c.getCorpEngName(), c.getStockCode());
		});

		// when
		List<CompanySearchResponse> result = companySearchService.search("test");

		// then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).corpName()).isEqualTo("테스트기업");
		assertThat(result.get(0).stockCode()).isEqualTo("000020");
	}
}
