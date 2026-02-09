package com.aivle.project.company.controller;

import com.aivle.project.common.dto.ApiResponse;
import com.aivle.project.company.dto.CompanyOverviewResponseDto;
import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.repository.CompaniesRepository;
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

	private static final String STOCK_CODE_PATTERN = "\\d{6}";

	private final CompanyOverviewService companyOverviewService;
	private final CompaniesRepository companiesRepository;

	@GetMapping("/{companyId}/overview")
	@Operation(
		summary = "기업 개요 조회",
		description = "Swagger에서 CompanyOverviewResponseDto위한 API입니다.",
		security = @SecurityRequirement(name = "bearerAuth")
	)
	public ResponseEntity<ApiResponse<CompanyOverviewResponseDto>> getOverview(
		@Parameter(description = "기업 ID 또는 종목 코드", example = "000020")
		@PathVariable("companyId") String companyId,
		@Parameter(description = "분기 키 (비어있으면 최신 분기 자동 조회, 예: 202401)", example = "202401")
		@RequestParam(value = "quarterKey", required = false) String quarterKey
	) {
		Long resolvedCompanyId = resolveCompanyId(companyId);
		CompanyOverviewResponseDto response = companyOverviewService.getOverview(resolvedCompanyId, quarterKey);
		return ResponseEntity.ok(ApiResponse.ok(response));
	}

	private Long resolveCompanyId(String companyIdOrCode) {
		if (companyIdOrCode == null || companyIdOrCode.isBlank()) {
			throw new IllegalArgumentException("Company id is required");
		}
		String trimmed = companyIdOrCode.trim();
		if (trimmed.chars().allMatch(Character::isDigit)) {
			Long id = Long.parseLong(trimmed);
			if (companiesRepository.existsById(id)) {
				return id;
			}
			if (trimmed.matches(STOCK_CODE_PATTERN)) {
				return companiesRepository.findByStockCode(trimmed)
					.map(CompaniesEntity::getId)
					.orElseThrow(() -> new IllegalArgumentException("Company not found for stockCode: " + trimmed));
			}
			throw new IllegalArgumentException("Company not found for id: " + trimmed);
		}
		return companiesRepository.findByStockCode(trimmed)
			.map(CompaniesEntity::getId)
			.orElseThrow(() -> new IllegalArgumentException("Company not found for stockCode: " + trimmed));
	}
}
