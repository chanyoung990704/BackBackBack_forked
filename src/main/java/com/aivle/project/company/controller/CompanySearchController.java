package com.aivle.project.company.controller;

import com.aivle.project.common.dto.ApiResponse;
import com.aivle.project.company.dto.CompanyInfoDto;
import com.aivle.project.company.dto.CompanySearchResponse;
import com.aivle.project.company.service.CompanySearchService;
import com.aivle.project.common.security.CurrentUser;
import com.aivle.project.watchlist.service.CompanyWatchlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 기업 검색 API.
 */
@Tag(name = "기업", description = "기업 검색")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/companies")
public class CompanySearchController {

	private final CompanySearchService companySearchService;
	private final CompanyWatchlistService companyWatchlistService;

	@GetMapping
	@Operation(summary = "내 워치리스트 기업 조회", description = "로그인 사용자의 watchlist 기업 목록을 반환합니다.")
	public ResponseEntity<ApiResponse<List<CompanyInfoDto>>> getMyCompanies(
		@Parameter(hidden = true) @CurrentUser Long userId
	) {
		List<CompanyInfoDto> response = companyWatchlistService.getWatchlistCompanies(userId);
		return ResponseEntity.ok(ApiResponse.ok(response));
	}

	@GetMapping("/search")
	@Operation(summary = "기업명 검색", description = "기업명/영문명 부분 일치로 기업을 검색합니다.")
	public ResponseEntity<ApiResponse<List<CompanySearchResponse>>> searchCompanies(
		@Parameter(description = "검색 키워드(2자 이상)", example = "삼성")
		@RequestParam("keyword") String keyword
	) {
		List<CompanySearchResponse> response = companySearchService.search(keyword);
		return ResponseEntity.ok(ApiResponse.ok(response));
	}
}
