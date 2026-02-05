package com.aivle.project.watchlist.controller;

import com.aivle.project.common.dto.ApiResponse;
import com.aivle.project.common.security.CurrentUser;
import com.aivle.project.risk.entity.RiskLevel;
import com.aivle.project.watchlist.dto.WatchlistAddRequest;
import com.aivle.project.watchlist.dto.WatchlistDashboardResponse;
import com.aivle.project.watchlist.service.CompanyWatchlistService;
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
public class CompanyWatchlistController {

	private final CompanyWatchlistService companyWatchlistService;

	@PostMapping
	public ResponseEntity<ApiResponse<Void>> add(@CurrentUser Long userId, @Valid @RequestBody WatchlistAddRequest request) {
		companyWatchlistService.addWatchlist(userId, request.companyId(), request.note());
		return ResponseEntity.ok(ApiResponse.ok(null));
	}

	@DeleteMapping("/{companyId}")
	public ResponseEntity<ApiResponse<Void>> remove(@CurrentUser Long userId, @PathVariable Long companyId) {
		companyWatchlistService.removeWatchlist(userId, companyId);
		return ResponseEntity.ok(ApiResponse.ok(null));
	}

	@GetMapping("/dashboard")
	public ResponseEntity<ApiResponse<WatchlistDashboardResponse>> dashboard(
		@CurrentUser Long userId,
		@RequestParam int year,
		@RequestParam int quarter,
		@RequestParam(required = false) List<String> metricCodes,
		@RequestParam(required = false) RiskLevel riskLevel
	) {
		WatchlistDashboardResponse response = companyWatchlistService.getDashboard(userId, year, quarter, metricCodes, riskLevel);
		return ResponseEntity.ok(ApiResponse.ok(response));
	}
}
