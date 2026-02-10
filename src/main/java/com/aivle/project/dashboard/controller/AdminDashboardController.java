package com.aivle.project.dashboard.controller;

import com.aivle.project.common.dto.ApiResponse;
import com.aivle.project.dashboard.dto.DashboardSummaryResponse;
import com.aivle.project.dashboard.service.DashboardSummaryService;
import com.aivle.project.user.service.AdminUserQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 대시보드 조회 API.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/dashboard")
@Tag(name = "Admin Dashboard", description = "관리자용 대시보드 조회 API")
@SecurityRequirement(name = "bearerAuth")
public class AdminDashboardController {

	private final DashboardSummaryService dashboardSummaryService;
	private final AdminUserQueryService adminUserQueryService;

	@GetMapping("/summary")
	@Operation(summary = "관리자 대시보드 요약 조회", description = "특정 userId 기준으로 대시보드 요약을 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "대상 사용자 없음")
	})
	public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary(
		@Parameter(description = "조회 대상 사용자 ID", example = "1")
		@RequestParam("userId") Long userId
	) {
		// 관리자 조회 대상 사용자의 유효성을 먼저 확인한다.
		adminUserQueryService.validateActiveUser(userId);
		DashboardSummaryResponse response = dashboardSummaryService.getSummary(userId);
		return ResponseEntity.ok(ApiResponse.ok(response));
	}
}
