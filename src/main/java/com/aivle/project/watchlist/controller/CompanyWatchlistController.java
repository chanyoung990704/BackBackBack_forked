package com.aivle.project.watchlist.controller;

import com.aivle.project.common.security.CurrentUser;
import com.aivle.project.risk.entity.RiskLevel;
import com.aivle.project.watchlist.dto.WatchlistAddRequest;
import com.aivle.project.watchlist.dto.WatchlistDashboardResponse;
import com.aivle.project.watchlist.dto.WatchlistMetricAveragesResponse;
import com.aivle.project.watchlist.dto.WatchlistMetricValuesResponse;
import com.aivle.project.watchlist.dto.WatchlistResponse;
import com.aivle.project.watchlist.service.CompanyWatchlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/watchlists")
@Tag(name = "Watchlist", description = "사용자 관심 기업(워치리스트) 관리 API")
@SecurityRequirement(name = "bearerAuth")
public class CompanyWatchlistController {

	private final CompanyWatchlistService companyWatchlistService;

	@PostMapping
	@Operation(summary = "워치리스트 등록", description = "사용자의 관심 기업을 워치리스트에 등록합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "등록 성공"),
		@ApiResponse(responseCode = "400", description = "요청 값 오류", content = @Content(schema = @Schema(implementation = com.aivle.project.common.dto.ApiResponse.class))),
		@ApiResponse(responseCode = "401", description = "인증 필요"),
		@ApiResponse(responseCode = "409", description = "이미 등록된 기업")
	})
	public ResponseEntity<com.aivle.project.common.dto.ApiResponse<Void>> add(@CurrentUser Long userId, @Valid @RequestBody WatchlistAddRequest request) {
		companyWatchlistService.addWatchlist(userId, request.companyId(), request.note());
		return ResponseEntity.ok(com.aivle.project.common.dto.ApiResponse.ok(null));
	}

	@GetMapping
	@Operation(summary = "워치리스트 목록 조회", description = "사용자가 등록한 워치리스트 기업 목록을 조회합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "401", description = "인증 필요")
	})
	public ResponseEntity<com.aivle.project.common.dto.ApiResponse<WatchlistResponse>> getWatchlist(@CurrentUser Long userId) {
		WatchlistResponse response = companyWatchlistService.getWatchlist(userId);
		return ResponseEntity.ok(com.aivle.project.common.dto.ApiResponse.ok(response));
	}

	@DeleteMapping("/{companyId}")
	@Operation(summary = "워치리스트 삭제", description = "사용자의 워치리스트에서 기업을 제거합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "삭제 성공"),
		@ApiResponse(responseCode = "401", description = "인증 필요"),
		@ApiResponse(responseCode = "404", description = "등록되지 않은 기업")
	})
	public ResponseEntity<com.aivle.project.common.dto.ApiResponse<Void>> remove(@CurrentUser Long userId, @PathVariable Long companyId) {
		companyWatchlistService.removeWatchlist(userId, companyId);
		return ResponseEntity.ok(com.aivle.project.common.dto.ApiResponse.ok(null));
	}

//	@GetMapping("/dashboard")
//	@Operation(summary = "워치리스트 대시보드 조회", description = "선택한 분기의 워치리스트 기업 지표/시장 평균/위험도를 조회합니다.")
//	@ApiResponses({
//		@ApiResponse(responseCode = "200", description = "조회 성공"),
//		@ApiResponse(responseCode = "400", description = "요청 값 오류"),
//		@ApiResponse(responseCode = "401", description = "인증 필요"),
//		@ApiResponse(responseCode = "404", description = "대상 분기 또는 데이터 없음")
//	})
//	public ResponseEntity<com.aivle.project.common.dto.ApiResponse<WatchlistDashboardResponse>> dashboard(
//		@CurrentUser Long userId,
//		@Parameter(description = "조회 연도", example = "2026") @RequestParam int year,
//		@Parameter(description = "조회 분기", example = "1") @RequestParam int quarter,
//		@Parameter(description = "조회할 지표 코드 목록(미입력 시 전체)", example = "ROE,OPM") @RequestParam(required = false) List<String> metricCodes,
//		@Parameter(description = "위험도 필터", example = "DANGER") @RequestParam(required = false) RiskLevel riskLevel
//	) {
//		WatchlistDashboardResponse response = companyWatchlistService.getDashboard(userId, year, quarter, metricCodes, riskLevel);
//		return ResponseEntity.ok(com.aivle.project.common.dto.ApiResponse.ok(response));
//	}

//	@GetMapping("/metric-averages")
//	@Operation(summary = "워치리스트 지표 평균 조회", description = "선택한 분기에 대해 내 워치리스트 기업들의 비위험 지표 평균(ACTUAL, 최신 발행 버전 기준)을 조회합니다.")
//	@ApiResponses({
//		@ApiResponse(responseCode = "200", description = "조회 성공"),
//		@ApiResponse(responseCode = "400", description = "요청 값 오류"),
//		@ApiResponse(responseCode = "401", description = "인증 필요")
//	})
//	public ResponseEntity<com.aivle.project.common.dto.ApiResponse<WatchlistMetricAveragesResponse>> metricAverages(
//		@CurrentUser Long userId,
//		@Parameter(description = "조회 연도", example = "2026") @RequestParam int year,
//		@Parameter(description = "조회 분기", example = "1") @RequestParam int quarter,
//		@Parameter(description = "조회할 지표 코드 목록(미입력 시 전체)", example = "ROE,ROA") @RequestParam(required = false) List<String> metricCodes
//	) {
//		WatchlistMetricAveragesResponse response = companyWatchlistService.getWatchlistMetricAverages(userId, year, quarter, metricCodes);
//		return ResponseEntity.ok(com.aivle.project.common.dto.ApiResponse.ok(response));
//	}

	@GetMapping("/metric-values")
	@Operation(summary = "워치리스트 지표 값 조회", description = "선택한 분기(또는 분기 범위)에 대해 워치리스트 기업들의 ACTUAL 지표 값을 최신 발행 버전 기준으로 조회합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "400", description = "요청 값 오류"),
		@ApiResponse(responseCode = "401", description = "인증 필요")
	})
	public ResponseEntity<com.aivle.project.common.dto.ApiResponse<WatchlistMetricValuesResponse>> metricValues(
		@CurrentUser Long userId,
		@Parameter(description = "조회 연도", example = "2026") @RequestParam(required = false) Integer year,
		@Parameter(description = "조회 분기", example = "1") @RequestParam(required = false) Integer quarter,
		@Parameter(description = "조회 시작 연도", example = "2024") @RequestParam(required = false) Integer fromYear,
		@Parameter(description = "조회 시작 분기", example = "1") @RequestParam(required = false) Integer fromQuarter,
		@Parameter(description = "조회 종료 연도", example = "2024") @RequestParam(required = false) Integer toYear,
		@Parameter(description = "조회 종료 분기", example = "4") @RequestParam(required = false) Integer toQuarter
	) {
		boolean hasRange = fromYear != null || fromQuarter != null || toYear != null || toQuarter != null;
		WatchlistMetricValuesResponse response;
		if (hasRange) {
			if (fromYear == null || fromQuarter == null || toYear == null || toQuarter == null) {
				throw new IllegalArgumentException("분기 범위 파라미터가 누락되었습니다.");
			}
			response = companyWatchlistService.getWatchlistMetricValuesByQuarterRange(
				userId,
				fromYear,
				fromQuarter,
				toYear,
				toQuarter
			);
		} else {
			if (year == null || quarter == null) {
				throw new IllegalArgumentException("분기 파라미터가 누락되었습니다.");
			}
			response = companyWatchlistService.getWatchlistMetricValuesByQuarter(userId, year, quarter);
		}
		return ResponseEntity.ok(com.aivle.project.common.dto.ApiResponse.ok(response));
	}
}
