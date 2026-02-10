package com.aivle.project.dashboard.controller;

import com.aivle.project.common.dto.ApiResponse;
import com.aivle.project.common.security.CurrentUser;
import com.aivle.project.dashboard.dto.CompanyQuarterRiskDto;
import com.aivle.project.dashboard.dto.DashboardSummaryResponse;
import com.aivle.project.dashboard.service.DashboardSummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 대시보드 API 컨트롤러.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "워치리스트 기반 대시보드 API")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

	private final DashboardSummaryService dashboardSummaryService;

	@GetMapping("/summary")
	@Operation(summary = "대시보드 요약 조회", description = "로그인 사용자의 워치리스트 기준 대시보드 요약을 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
	})
	public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary(@CurrentUser Long userId) {
		DashboardSummaryResponse response = dashboardSummaryService.getSummary(userId);
		return ResponseEntity.ok(ApiResponse.ok(response));
	}

	@GetMapping("/risk-records")
	@Operation(summary = "대시보드 리스크 레코드 조회", description = "로그인 사용자의 워치리스트 기준 ACTUAL 리스크 레코드를 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
	})
	public ResponseEntity<ApiResponse<List<CompanyQuarterRiskDto>>> getRiskRecords(
		@CurrentUser Long userId,
		@RequestParam(name = "limit", defaultValue = "200") int limit
	) {
		int sanitizedLimit = Math.max(1, Math.min(limit, 1000));
		List<CompanyQuarterRiskDto> response = dashboardSummaryService.getRiskRecords(userId, sanitizedLimit);
		return ResponseEntity.ok(ApiResponse.ok(response));
	}
}
