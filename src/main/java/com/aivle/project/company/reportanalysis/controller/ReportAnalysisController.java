package com.aivle.project.company.reportanalysis.controller;

import com.aivle.project.common.dto.ApiResponse;
import com.aivle.project.company.reportanalysis.dto.ReportAnalysisResponse;
import com.aivle.project.company.reportanalysis.service.ReportAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사업보고서 분석 API 컨트롤러.
 */
@Tag(name = "사업보고서", description = "사업보고서 분석 데이터")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/companies")
public class ReportAnalysisController {

	private final ReportAnalysisService reportAnalysisService;

	/**
	 * AI 서버에서 사업보고서 분석 데이터를 가져와 저장합니다.
	 *
	 * @param companyId 기업 식별자(현재는 stock_code도 허용)
	 * @return 저장된 사업보고서 분석 정보
	 */
	@PostMapping({"/{companyId}/reports/sync", "/{companyId}/report/fetch"})
	@Operation(summary = "사업보고서 분석 수집", description = "AI 서버에서 사업보고서 분석 데이터를 가져와 저장합니다.")
	@SecurityRequirement(name = "bearerAuth")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "사업보고서 분석 수집 및 저장 성공",
			content = @Content(schema = @Schema(implementation = com.aivle.project.common.dto.ApiResponse.class))),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "기업을 찾을 수 없음")
	})
	public ResponseEntity<ApiResponse<ReportAnalysisResponse>> fetchReportAnalysis(
		@Parameter(description = "기업 식별자(companyId 또는 stock_code)", example = "000020")
		@PathVariable String companyId
	) {
		ReportAnalysisResponse result = reportAnalysisService.fetchAndStoreReport(companyId);
		return ResponseEntity.ok(ApiResponse.ok(result));
	}

	/**
	 * 특정 기업의 최신 사업보고서 분석을 조회합니다.
	 *
	 * @param companyId 기업 식별자(현재는 stock_code도 허용)
	 * @return 최신 사업보고서 분석 정보
	 */
	@GetMapping({"/{companyId}/reports/latest", "/{companyId}/report/latest"})
	@Operation(summary = "최신 사업보고서 분석 조회", description = "기업의 최신 사업보고서 분석 결과를 조회합니다.")
	@SecurityRequirement(name = "bearerAuth")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "최신 사업보고서 분석 조회 성공",
			content = @Content(schema = @Schema(implementation = com.aivle.project.common.dto.ApiResponse.class))),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "기업을 찾을 수 없음")
	})
	public ResponseEntity<ApiResponse<ReportAnalysisResponse>> getLatestReportAnalysis(
		@Parameter(description = "기업 식별자(companyId 또는 stock_code)", example = "000020")
		@PathVariable String companyId
	) {
		ReportAnalysisResponse result = reportAnalysisService.getLatestReport(companyId);
		if (result == null) {
			return ResponseEntity.ok(ApiResponse.fail(
				com.aivle.project.common.error.ErrorResponse.of(
					"NO_REPORT_DATA",
					"사업보고서 분석 데이터가 없습니다.",
					"/api/companies/" + companyId + "/reports/latest"
				)
			));
		}
		return ResponseEntity.ok(ApiResponse.ok(result));
	}
}
