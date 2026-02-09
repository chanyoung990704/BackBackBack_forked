package com.aivle.project.company.dto;

import java.util.List;

/**
 * AI 코멘트 수동 캐시 적재 요청 DTO.
 */
public record AdminAiCommentCacheRequest(
	List<Long> companyIds,
	String period
) {
}
