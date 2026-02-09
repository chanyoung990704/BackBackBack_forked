package com.aivle.project.company.controller;

import com.aivle.project.common.dto.ApiResponse;
import com.aivle.project.company.dto.AdminAiCommentCacheRequest;
import com.aivle.project.company.service.CompanyAiCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 수동 AI 코멘트 캐시 API.
 */
@Tag(name = "Admin Company AI Comment", description = "관리자용 AI 코멘트 캐시 API")
@RestController
@RequestMapping("/api/admin/companies")
@RequiredArgsConstructor
public class AdminCompanyAiCommentController {

	private final CompanyAiCommentService companyAiCommentService;

	@Operation(summary = "기업 AI 코멘트 수동 캐시", description = "company_id 기준으로 AI 코멘트를 수동 캐시합니다.")
	@PostMapping("/{companyId}/ai-comment/cache")
	public ApiResponse<String> cacheSingle(
		@PathVariable Long companyId,
		@RequestParam(value = "period", required = false) String period
	) {
		companyAiCommentService.ensureAiCommentCached(companyId, period);
		return ApiResponse.ok("AI 코멘트 캐시가 완료되었습니다.");
	}

	@Operation(summary = "기업 AI 코멘트 수동 일괄 캐시", description = "복수 company_id에 대해 AI 코멘트를 수동 캐시합니다.")
	@PostMapping("/ai-comment/cache")
	public ApiResponse<List<Long>> cacheBulk(@Valid @RequestBody AdminAiCommentCacheRequest request) {
		List<Long> processedIds = new ArrayList<>();
		if (request == null || request.companyIds() == null || request.companyIds().isEmpty()) {
			return ApiResponse.ok(processedIds);
		}

		for (Long companyId : request.companyIds()) {
			companyAiCommentService.ensureAiCommentCached(companyId, request.period());
			processedIds.add(companyId);
		}
		return ApiResponse.ok(processedIds);
	}
}
