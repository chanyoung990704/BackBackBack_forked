package com.aivle.project.company.service;

import com.aivle.project.company.dto.CompanySearchResponse;
import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.repository.CompaniesRepository;
import java.sql.SQLException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * 기업 검색 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompanySearchService {

	private static final int MIN_KEYWORD_LENGTH = 2;
	private static final int SEARCH_LIMIT = 20;

	private final CompaniesRepository companiesRepository;
	private final com.aivle.project.company.mapper.CompanyMapper companyMapper;
	@Value("${spring.datasource.url:}")
	private String dataSourceUrl;

	public List<CompanySearchResponse> search(String keyword) {
		String normalized = normalizeKeyword(keyword);
		if (normalized.length() < MIN_KEYWORD_LENGTH) {
			throw new IllegalArgumentException("keyword는 2자 이상이어야 합니다.");
		}

		List<CompaniesEntity> companies = searchHybrid(normalized);
		log.info("기업 검색 완료: keyword={}, count={}, datasource={}", normalized, companies.size(), dataSourceUrl);
		return companies.stream()
			.map(companyMapper::toSearchResponse)
			.toList();
	}

	private List<CompaniesEntity> searchHybrid(String keyword) {
		if (isH2Database()) {
			return searchWithLike(keyword);
		}

		try {
			List<CompaniesEntity> fullTextResults = searchWithFullText(keyword);
			if (!fullTextResults.isEmpty()) {
				return fullTextResults;
			}
			// FULLTEXT 결과가 비어 있으면 부분일치 LIKE로 보완한다.
			log.info("기업 검색 FULLTEXT 결과 0건으로 LIKE fallback 수행: keyword={}", keyword);
			return searchWithLike(keyword);
		} catch (DataAccessException ex) {
			if (!shouldFallbackOn(ex)) {
				throw ex;
			}
			log.warn("기업 검색 FULLTEXT 실패로 LIKE fallback 수행: keyword={}, message={}", keyword, ex.getMessage());
			return searchWithLike(keyword);
		}
	}

	private List<CompaniesEntity> searchWithFullText(String keyword) {
		return companiesRepository.searchByKeywordFullTextExcludingNullStockCode(keyword, SEARCH_LIMIT);
	}

	private List<CompaniesEntity> searchWithLike(String keyword) {
		return companiesRepository.searchByKeywordExcludingNullStockCode(
			keyword,
			PageRequest.of(0, SEARCH_LIMIT)
		);
	}

	private boolean shouldFallbackOn(RuntimeException ex) {
		Throwable root = ex;
		while (root.getCause() != null) {
			root = root.getCause();
		}
		if (root instanceof SQLException sqlException) {
			return sqlException.getErrorCode() == 1191 || sqlException.getSQLState() != null;
		}
		return true;
	}

	// 테스트(H2)에서는 MATCH AGAINST가 미지원이므로 LIKE 쿼리로 분기한다.
	private boolean isH2Database() {
		return dataSourceUrl != null && dataSourceUrl.startsWith("jdbc:h2:");
	}

	private String normalizeKeyword(String keyword) {
		if (keyword == null) {
			return "";
		}
		return keyword.trim();
	}
}
