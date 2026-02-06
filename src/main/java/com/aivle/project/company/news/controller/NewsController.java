package com.aivle.project.company.news.controller;

import com.aivle.project.common.dto.ApiResponse;
import com.aivle.project.company.news.dto.NewsAnalysisResponse;
import com.aivle.project.company.news.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 뉴스 분석 API 컨트롤러.
 */
@Tag(name = "뉴스", description = "뉴스 분석 데이터")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/companies")
public class NewsController {

    private final NewsService newsService;

    /**
     * AI 서버에서 뉴스 분석 데이터를 가져와 저장합니다.
     *
     * @param stockCode 기업 코드 (stock_code)
     * @return 저장된 뉴스 분석 정보
     */
    @PostMapping("/{stockCode}/news/fetch")
    @Operation(summary = "뉴스 분석 수집", description = "AI 서버에서 뉴스 분석 데이터를 가져와 저장합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "뉴스 수집 및 저장 성공",
                    content = @Content(schema = @Schema(implementation = com.aivle.project.common.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "기업을 찾을 수 없음")
    })
    public ResponseEntity<ApiResponse<NewsAnalysisResponse>> fetchNews(
            @Parameter(description = "기업 코드 (stock_code)", example = "000020")
            @PathVariable String stockCode
    ) {
        NewsAnalysisResponse result = newsService.fetchAndStoreNews(stockCode);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * 특정 기업의 최신 뉴스를 조회합니다.
     *
     * @param stockCode 기업 코드 (stock_code)
     * @return 최신 뉴스 분석 정보
     */
    @GetMapping("/{stockCode}/news/latest")
    @Operation(summary = "최신 뉴스 조회", description = "기업의 최신 뉴스 분석 결과를 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "최신 뉴스 조회 성공",
                    content = @Content(schema = @Schema(implementation = com.aivle.project.common.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "기업을 찾을 수 없음")
    })
    public ResponseEntity<ApiResponse<NewsAnalysisResponse>> getLatestNews(
            @Parameter(description = "기업 코드 (stock_code)", example = "000020")
            @PathVariable String stockCode
    ) {
        NewsAnalysisResponse result = newsService.getLatestNews(stockCode);
        if (result == null) {
            return ResponseEntity.ok(ApiResponse.fail(
                    com.aivle.project.common.error.ErrorResponse.of(
                            "NO_NEWS_DATA",
                            "뉴스 데이터가 없습니다.",
                            "/api/companies/" + stockCode + "/news/latest"
                    )
            ));
        }
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * 특정 기간의 뉴스 분석 이력을 조회합니다.
     *
     * @param stockCode 기업 코드 (stock_code)
     * @param start     시작 일시 (ISO-8601)
     * @param end       종료 일시 (ISO-8601)
     * @return 뉴스 분석 이력 목록
     */
	@GetMapping("/{stockCode}/news/history")
	@Operation(summary = "뉴스 분석 이력 조회", description = "특정 기간의 뉴스 분석 이력을 조회합니다.")
	@SecurityRequirement(name = "bearerAuth")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "뉴스 이력 조회 성공",
			content = @Content(schema = @Schema(implementation = com.aivle.project.common.dto.ApiResponse.class))),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "기업을 찾을 수 없음")
	})
	public ResponseEntity<ApiResponse<java.util.List<NewsAnalysisResponse>>> getNewsHistory(
		@Parameter(description = "기업 코드 (stock_code)", example = "000020")
		@PathVariable String stockCode,
		@Parameter(description = "시작 일시 (ISO-8601)", example = "2026-01-01T00:00:00")
		@RequestParam(required = false)
		@org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
		LocalDateTime start,
		@Parameter(description = "종료 일시 (ISO-8601)", example = "2026-12-31T23:59:59")
		@RequestParam(required = false)
		@org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
		LocalDateTime end
	) {
		LocalDateTime startDate = start != null ? start : LocalDateTime.now().minusMonths(1);
		LocalDateTime endDate = end != null ? end : LocalDateTime.now();

		java.util.List<NewsAnalysisResponse> result = newsService.getNewsHistory(stockCode, startDate, endDate);
		return ResponseEntity.ok(ApiResponse.ok(result));
	}
}
