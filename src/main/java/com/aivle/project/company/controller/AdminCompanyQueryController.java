package com.aivle.project.company.controller;

import com.aivle.project.common.dto.ApiResponse;
import com.aivle.project.company.dto.CompanyInfoDto;
import com.aivle.project.company.dto.CompanyOverviewResponseDto;
import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.insight.dto.CompanyInsightResponseDto;
import com.aivle.project.company.insight.service.CompanyInsightService;
import com.aivle.project.company.repository.CompaniesRepository;
import com.aivle.project.company.service.CompanyOverviewService;
import com.aivle.project.user.service.AdminUserQueryService;
import com.aivle.project.watchlist.service.CompanyWatchlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 기업 조회 API.
 */
@Tag(name = "Admin Company Query", description = "관리자용 사용자 기준 기업 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/companies")
@SecurityRequirement(name = "bearerAuth")
public class AdminCompanyQueryController {

	private static final String STOCK_CODE_PATTERN = "\\d{6}";
	private static final int DEFAULT_NEWS_PAGE = 0;
	private static final int DEFAULT_NEWS_SIZE = 10;
	private static final int DEFAULT_REPORT_PAGE = 0;
	private static final int DEFAULT_REPORT_SIZE = 1;

	private final AdminUserQueryService adminUserQueryService;
	private final CompanyWatchlistService companyWatchlistService;
	private final CompanyOverviewService companyOverviewService;
	private final CompanyInsightService companyInsightService;
	private final CompaniesRepository companiesRepository;

	@GetMapping
	@Operation(summary = "관리자 사용자 기준 워치리스트 기업 조회", description = "특정 userId의 워치리스트 기업 목록을 조회합니다.")
	public ResponseEntity<ApiResponse<List<CompanyInfoDto>>> getCompanies(
		@Parameter(description = "조회 대상 사용자 ID", example = "1")
		@RequestParam("userId") Long userId
	) {
		adminUserQueryService.validateActiveUser(userId);
		List<CompanyInfoDto> response = companyWatchlistService.getWatchlistCompanies(userId);
		return ResponseEntity.ok(ApiResponse.ok(response));
	}

	@GetMapping("/{companyId}/overview")
	@Operation(summary = "관리자 사용자 기준 기업 개요 조회", description = "특정 userId 기준으로 기업 개요를 조회합니다.")
	public ResponseEntity<ApiResponse<CompanyOverviewResponseDto>> getOverview(
		@Parameter(description = "기업 ID 또는 종목 코드", example = "000020")
		@PathVariable("companyId") String companyId,
		@Parameter(description = "조회 대상 사용자 ID", example = "1")
		@RequestParam("userId") Long userId,
		@Parameter(description = "분기 키 (비어있으면 최신 분기 자동 조회, 예: 202401)", example = "202401")
		@RequestParam(value = "quarterKey", required = false) String quarterKey
	) {
		adminUserQueryService.validateActiveUser(userId);
		Long resolvedCompanyId = resolveCompanyId(companyId);
		CompanyOverviewResponseDto response = companyOverviewService.getOverview(resolvedCompanyId, quarterKey);
		return ResponseEntity.ok(ApiResponse.ok(response));
	}

	@GetMapping("/{companyId}/insights")
	@Operation(summary = "관리자 사용자 기준 기업 인사이트 조회", description = "특정 userId 기준으로 기업 인사이트를 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인사이트 조회 성공",
			content = @Content(schema = @Schema(implementation = com.aivle.project.common.dto.ApiResponse.class))),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "인사이트 생성 중"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "대상 사용자 또는 기업을 찾을 수 없음")
	})
	public ResponseEntity<ApiResponse<CompanyInsightResponseDto>> getInsights(
		@Parameter(description = "기업 ID", example = "100")
		@PathVariable Long companyId,
		@Parameter(description = "조회 대상 사용자 ID", example = "1")
		@RequestParam("userId") Long userId,
		@Parameter(description = "뉴스 페이지", example = "0")
		@RequestParam(required = false) Integer newsPage,
		@Parameter(description = "뉴스 페이지 크기", example = "10")
		@RequestParam(required = false) Integer newsSize,
		@Parameter(description = "보고서 페이지", example = "0")
		@RequestParam(required = false) Integer reportPage,
		@Parameter(description = "보고서 페이지 크기", example = "1")
		@RequestParam(required = false) Integer reportSize,
		@Parameter(description = "뉴스/사업보고서 최신 데이터 강제 재수집 여부", example = "false")
		@RequestParam(required = false, defaultValue = "false") boolean refresh
	) {
		adminUserQueryService.validateActiveUser(userId);
		int resolvedNewsPage = normalizePage(newsPage, DEFAULT_NEWS_PAGE);
		int resolvedNewsSize = normalizeSize(newsSize, DEFAULT_NEWS_SIZE);
		int resolvedReportPage = normalizePage(reportPage, DEFAULT_REPORT_PAGE);
		int resolvedReportSize = normalizeSize(reportSize, DEFAULT_REPORT_SIZE);

		CompanyInsightService.InsightResult result = companyInsightService.getInsights(
			companyId,
			resolvedNewsPage,
			resolvedNewsSize,
			resolvedReportPage,
			resolvedReportSize,
			refresh
		);
		if (result.processing()) {
			return ResponseEntity.accepted().body(ApiResponse.fail(
				com.aivle.project.common.error.ErrorResponse.of(
					"INSIGHT_PROCESSING",
					"인사이트 데이터 생성 중입니다. 잠시 후 다시 시도해 주세요.",
					"/api/admin/companies/" + companyId + "/insights"
				)
			));
		}
		CompanyInsightResponseDto response = CompanyInsightResponseDto.builder()
			.averageScore(result.averageScore())
			.items(result.items())
			.build();
		return ResponseEntity.ok(ApiResponse.ok(response));
	}

	private int normalizePage(Integer page, int defaultValue) {
		if (page == null || page < 0) {
			return defaultValue;
		}
		return page;
	}

	private int normalizeSize(Integer size, int defaultValue) {
		if (size == null || size <= 0) {
			return defaultValue;
		}
		return Math.min(size, 50);
	}

	private Long resolveCompanyId(String companyIdOrCode) {
		if (companyIdOrCode == null || companyIdOrCode.isBlank()) {
			throw new IllegalArgumentException("Company id is required");
		}
		String trimmed = companyIdOrCode.trim();
		if (trimmed.chars().allMatch(Character::isDigit)) {
			Long id = Long.parseLong(trimmed);
			if (companiesRepository.existsById(id)) {
				return id;
			}
			if (trimmed.matches(STOCK_CODE_PATTERN)) {
				return companiesRepository.findByStockCode(trimmed)
					.map(CompaniesEntity::getId)
					.orElseThrow(() -> new IllegalArgumentException("Company not found for stockCode: " + trimmed));
			}
			throw new IllegalArgumentException("Company not found for id: " + trimmed);
		}
		return companiesRepository.findByStockCode(trimmed)
			.map(CompaniesEntity::getId)
			.orElseThrow(() -> new IllegalArgumentException("Company not found for stockCode: " + trimmed));
	}
}
