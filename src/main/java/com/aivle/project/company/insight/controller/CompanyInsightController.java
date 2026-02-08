package com.aivle.project.company.insight.controller;

import com.aivle.project.common.dto.ApiResponse;
import com.aivle.project.company.insight.dto.CompanyInsightDto;
import com.aivle.project.company.insight.service.CompanyInsightService;
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
 * 기업 인사이트 API 컨트롤러.
 */
@Tag(name = "인사이트", description = "기업 최신 뉴스/사업보고서 인사이트")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/companies")
public class CompanyInsightController {

	private static final int DEFAULT_NEWS_PAGE = 0;
	private static final int DEFAULT_NEWS_SIZE = 10;
	private static final int DEFAULT_REPORT_PAGE = 0;
	private static final int DEFAULT_REPORT_SIZE = 1;

	private final CompanyInsightService companyInsightService;

	@GetMapping("/{companyId}/insights")
	@Operation(summary = "기업 인사이트 조회", description = "기업의 최신 뉴스와 사업보고서 인사이트를 조회합니다.")
	@SecurityRequirement(name = "bearerAuth")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인사이트 조회 성공",
			content = @Content(schema = @Schema(implementation = com.aivle.project.common.dto.ApiResponse.class))),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "기업을 찾을 수 없음")
	})
	public ResponseEntity<ApiResponse<List<CompanyInsightDto>>> getCompanyInsights(
		@Parameter(description = "기업 ID", example = "100")
		@PathVariable Long companyId,
		@Parameter(description = "뉴스 페이지", example = "0")
		@RequestParam(required = false) Integer newsPage,
		@Parameter(description = "뉴스 페이지 크기", example = "10")
		@RequestParam(required = false) Integer newsSize,
		@Parameter(description = "보고서 페이지", example = "0")
		@RequestParam(required = false) Integer reportPage,
		@Parameter(description = "보고서 페이지 크기", example = "1")
		@RequestParam(required = false) Integer reportSize
	) {
		int resolvedNewsPage = normalizePage(newsPage, DEFAULT_NEWS_PAGE);
		int resolvedNewsSize = normalizeSize(newsSize, DEFAULT_NEWS_SIZE);
		int resolvedReportPage = normalizePage(reportPage, DEFAULT_REPORT_PAGE);
		int resolvedReportSize = normalizeSize(reportSize, DEFAULT_REPORT_SIZE);

		CompanyInsightService.InsightResult result = companyInsightService.getInsights(
			companyId,
			resolvedNewsPage,
			resolvedNewsSize,
			resolvedReportPage,
			resolvedReportSize
		);
		if (result.processing()) {
			return ResponseEntity.accepted().body(ApiResponse.fail(
				com.aivle.project.common.error.ErrorResponse.of(
					"INSIGHT_PROCESSING",
					"인사이트 데이터 생성 중입니다. 잠시 후 다시 시도해 주세요.",
					"/api/companies/" + companyId + "/insights"
				)
			));
		}
		return ResponseEntity.ok(ApiResponse.ok(result.items()));
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
}
