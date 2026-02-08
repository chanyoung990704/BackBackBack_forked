package com.aivle.project.report.controller;

import com.aivle.project.common.dto.ApiResponse;
import com.aivle.project.report.dto.ReportLatestPredictResponse;
import com.aivle.project.report.dto.ReportMetricGroupedResponse;
import com.aivle.project.report.service.CompanyReportMetricQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 보고서 지표 조회 API.
 */
//@Tag(name = "보고서 지표", description = "보고서 지표 조회")
//@RestController
//@RequiredArgsConstructor
//@RequestMapping("/api/reports/metrics")
//@SecurityRequirement(name = "bearerAuth")
//public class ReportMetricQueryController {
//
//	private final CompanyReportMetricQueryService companyReportMetricQueryService;
//
//	@GetMapping("/grouped")
//	@Operation(summary = "보고서 지표 분기별 조회", description = "기업의 최신 버전 지표를 분기별로 그룹핑하여 조회합니다.")
//	public ResponseEntity<ApiResponse<ReportMetricGroupedResponse>> fetchGroupedMetrics(
//		@Parameter(description = "기업코드(stock_code)", example = "000020")
//		@RequestParam("stockCode") String stockCode,
//		@Parameter(description = "조회 시작 분기키(YYYYQ)", example = "20244")
//		@RequestParam("fromQuarterKey") int fromQuarterKey,
//		@Parameter(description = "조회 종료 분기키(YYYYQ)", example = "20253")
//		@RequestParam("toQuarterKey") int toQuarterKey
//	) {
//		ReportMetricGroupedResponse response = companyReportMetricQueryService
//			.fetchLatestMetricsGrouped(stockCode, fromQuarterKey, toQuarterKey);
//		return ResponseEntity.ok(ApiResponse.ok(response));
//	}
//
//	@GetMapping("/predict-latest")
//	@Operation(summary = "최신 예측 지표 조회", description = "기업/분기 기준 최신 예측 지표와 PDF 정보를 조회합니다.")
//	public ResponseEntity<ApiResponse<ReportLatestPredictResponse>> fetchLatestPredictMetrics(
//		@Parameter(description = "기업코드(stock_code)", example = "000020")
//		@RequestParam("stockCode") String stockCode,
//		@Parameter(description = "분기키(YYYYQ)", example = "20253")
//		@RequestParam("quarterKey") int quarterKey
//	) {
//		ReportLatestPredictResponse response = companyReportMetricQueryService
//			.fetchLatestPredictMetrics(stockCode, quarterKey);
//		return ResponseEntity.ok(ApiResponse.ok(response));
//	}
//}
