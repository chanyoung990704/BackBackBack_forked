package com.aivle.project.company.news.controller;

import com.aivle.project.common.dto.ApiResponse;
import com.aivle.project.company.news.dto.NewsRefreshResponse;
import com.aivle.project.company.news.service.NewsService;
import com.aivle.project.company.service.CompanyReputationScoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 뉴스 재수집 API 컨트롤러.
 */
@Tag(name = "뉴스", description = "뉴스 재수집/복구")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/companies")
public class NewsAdminController {

	private final NewsService newsService;
	private final CompanyReputationScoreService companyReputationScoreService;

	@PostMapping({"/{companyId}/news/refresh", "/{companyId}/news/refresh-latest"})
	@Operation(summary = "최신 뉴스 재수집", description = "최신 뉴스를 재수집하고 average_score 누락 시 평균값을 복구합니다.")
	@SecurityRequirement(name = "bearerAuth")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "뉴스 재수집 및 복구 성공",
			content = @Content(schema = @Schema(implementation = com.aivle.project.common.dto.ApiResponse.class))),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "기업을 찾을 수 없음")
	})
	public ResponseEntity<ApiResponse<NewsRefreshResponse>> refreshLatestNews(
		@Parameter(description = "기업 식별자(companyId 또는 stock_code)", example = "000020")
		@PathVariable String companyId
	) {
		NewsRefreshResponse result = newsService.refreshLatestNews(companyId);
		// 최신 뉴스 재수집 후 연결 테이블(company_key_metrics.external_health_score)도 최신 분기로 동기화한다.
		companyReputationScoreService.syncExternalHealthScoreIfPresent(result.analysis().companyId(), companyId);
		return ResponseEntity.ok(ApiResponse.ok(result));
	}
}
