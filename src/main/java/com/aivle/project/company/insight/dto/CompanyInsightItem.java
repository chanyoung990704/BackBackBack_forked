package com.aivle.project.company.insight.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 기업 인사이트 항목 DTO.
 */
@Schema(description = "기업 인사이트 항목")
public record CompanyInsightItem(
	@Schema(description = "ID", example = "1")
	Long id,

	@Schema(description = "타입", example = "REPORT")
	CompanyInsightType type,

	@Schema(description = "제목")
	String title,

	@Schema(description = "본문 요약")
	String body,

	@Schema(description = "본문")
	String content,

	@Schema(description = "출처")
	String source,

	@Schema(description = "발행 시각 (UTC)")
	String publishedAt,

	@Schema(description = "원문 URL")
	String url
) {
}
