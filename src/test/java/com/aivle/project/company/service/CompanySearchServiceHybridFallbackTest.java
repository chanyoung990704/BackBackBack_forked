package com.aivle.project.company.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.aivle.project.company.dto.CompanySearchResponse;
import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.mapper.CompanyMapper;
import com.aivle.project.company.repository.CompaniesRepository;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CompanySearchServiceHybridFallbackTest {

	@InjectMocks
	private CompanySearchService companySearchService;

	@Mock
	private CompaniesRepository companiesRepository;

	@Mock
	private CompanyMapper companyMapper;

	@Test
	@DisplayName("MySQL에서 FULLTEXT 결과가 있으면 그대로 반환한다")
	void search_UsesFullTextWhenResultExists() {
		// given
		setDatasourceUrl("jdbc:mysql://localhost:3306/bigprj");
		CompaniesEntity company = company("케이티", "KT", "030200");
		given(companiesRepository.searchByKeywordFullTextExcludingNullStockCode("케이티", 20))
			.willReturn(List.of(company));
		given(companyMapper.toSearchResponse(company))
			.willReturn(new CompanySearchResponse(1L, "케이티", "KT", "030200"));

		// when
		List<CompanySearchResponse> result = companySearchService.search("케이티");

		// then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).stockCode()).isEqualTo("030200");
		verifyNoInteractionsOnLikeQuery();
	}

	@Test
	@DisplayName("MySQL에서 FULLTEXT 결과가 0건이면 LIKE로 fallback한다")
	void search_FallbacksToLikeWhenFullTextEmpty() {
		// given
		setDatasourceUrl("jdbc:mysql://localhost:3306/bigprj");
		CompaniesEntity company = company("케이티", "KT", "030200");
		given(companiesRepository.searchByKeywordFullTextExcludingNullStockCode("케이티", 20))
			.willReturn(List.of());
		given(companiesRepository.searchByKeywordExcludingNullStockCode(
			org.mockito.ArgumentMatchers.eq("케이티"),
			org.mockito.ArgumentMatchers.any(Pageable.class)
		)).willReturn(List.of(company));
		given(companyMapper.toSearchResponse(company))
			.willReturn(new CompanySearchResponse(1L, "케이티", "KT", "030200"));

		// when
		List<CompanySearchResponse> result = companySearchService.search("케이티");

		// then
		assertThat(result).hasSize(1);
		verify(companiesRepository).searchByKeywordExcludingNullStockCode(
			org.mockito.ArgumentMatchers.eq("케이티"),
			org.mockito.ArgumentMatchers.any(Pageable.class)
		);
	}

	@Test
	@DisplayName("MySQL에서 FULLTEXT 예외가 발생하면 LIKE로 fallback한다")
	void search_FallbacksToLikeWhenFullTextThrows() {
		// given
		setDatasourceUrl("jdbc:mysql://localhost:3306/bigprj");
		CompaniesEntity company = company("케이티", "KT", "030200");
		JpaSystemException fullTextException = new JpaSystemException(
			new RuntimeException(new SQLException("Can't find FULLTEXT index matching the column list", "HY000", 1191))
		);
		given(companiesRepository.searchByKeywordFullTextExcludingNullStockCode("케이티", 20))
			.willThrow(fullTextException);
		given(companiesRepository.searchByKeywordExcludingNullStockCode(
			org.mockito.ArgumentMatchers.eq("케이티"),
			org.mockito.ArgumentMatchers.any(Pageable.class)
		)).willReturn(List.of(company));
		given(companyMapper.toSearchResponse(company))
			.willReturn(new CompanySearchResponse(1L, "케이티", "KT", "030200"));

		// when
		List<CompanySearchResponse> result = companySearchService.search("케이티");

		// then
		assertThat(result).hasSize(1);
		verify(companiesRepository).searchByKeywordExcludingNullStockCode(
			org.mockito.ArgumentMatchers.eq("케이티"),
			org.mockito.ArgumentMatchers.any(Pageable.class)
		);
	}

	@Test
	@DisplayName("H2에서는 LIKE 검색만 사용한다")
	void search_UsesLikeOnlyOnH2() {
		// given
		setDatasourceUrl("jdbc:h2:mem:testdb");
		CompaniesEntity company = company("케이티", "KT", "030200");
		given(companiesRepository.searchByKeywordExcludingNullStockCode(
			org.mockito.ArgumentMatchers.eq("케이티"),
			org.mockito.ArgumentMatchers.any(Pageable.class)
		)).willReturn(List.of(company));
		given(companyMapper.toSearchResponse(company))
			.willReturn(new CompanySearchResponse(1L, "케이티", "KT", "030200"));

		// when
		List<CompanySearchResponse> result = companySearchService.search("케이티");

		// then
		assertThat(result).hasSize(1);
		verify(companiesRepository, never()).searchByKeywordFullTextExcludingNullStockCode("케이티", 20);
		verify(companiesRepository).searchByKeywordExcludingNullStockCode(
			org.mockito.ArgumentMatchers.eq("케이티"),
			org.mockito.ArgumentMatchers.any(Pageable.class)
		);
	}

	private void verifyNoInteractionsOnLikeQuery() {
		verify(companiesRepository).searchByKeywordFullTextExcludingNullStockCode("케이티", 20);
		verify(companiesRepository, never()).searchByKeywordExcludingNullStockCode(
			org.mockito.ArgumentMatchers.eq("케이티"),
			org.mockito.ArgumentMatchers.any(Pageable.class)
		);
	}

	private void setDatasourceUrl(String url) {
		ReflectionTestUtils.setField(companySearchService, "dataSourceUrl", url);
	}

	private CompaniesEntity company(String corpName, String corpEngName, String stockCode) {
		return CompaniesEntity.create("00000001", corpName, corpEngName, stockCode, LocalDate.of(2025, 1, 1));
	}
}
