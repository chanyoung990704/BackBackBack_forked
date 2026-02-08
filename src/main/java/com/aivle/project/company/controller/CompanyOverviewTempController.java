package com.aivle.project.company.controller;

import com.aivle.project.common.dto.ApiResponse;
import com.aivle.project.company.dto.CompanyOverviewResponseDto;
import com.aivle.project.company.service.CompanyOverviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 기업 개요 API.
 */
@Tag(name = "기업", description = "기업 개요")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/companies")
public class CompanyOverviewTempController {

	private final CompanyOverviewService companyOverviewService;

	@GetMapping("/{companyId}/overview")
	@Operation(
		summary = "기업 개요 조회",
		description = "Swagger에서 CompanyOverviewResponseDto위한 API입니다.",
		security = @SecurityRequirement(name = "bearerAuth")
	)
	public ResponseEntity<ApiResponse<CompanyOverviewResponseDto>> getOverview(
		@Parameter(description = "기업 ID", example = "1")
		@PathVariable("companyId") Long companyId,
		@Parameter(description = "분기 키 (예: 202401)", example = "202401")
		@RequestParam("quarterKey") String quarterKey
	) {
		CompanyOverviewResponseDto response = companyOverviewService.getOverview(companyId, quarterKey);
		return ResponseEntity.ok(ApiResponse.ok(response));
	}
}
