package com.aivle.project.company.reportanalysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * 사업보고서 요약 항목 DTO.
 */
@Schema(description = "사업보고서 요약 항목")
public record ReportContentDto(
	@Schema(description = "항목 ID", example = "1")
	Long id,

	@Schema(description = "제목", example = "동화약품 2025년 3분기 사업내용")
	String title,

	@Schema(description = "요약")
	String summary,

	@Schema(description = "점수", example = "0.12")
	Double score,

	@Schema(description = "발행 시각 (UTC)", example = "2026-02-05T12:35:41.369155Z")
	OffsetDateTime publishedAt,

	@Schema(description = "원문 URL")
	String link,

	@Schema(description = "감성 분류", example = "NEU")
	String sentiment
) {

	public static ReportContentDto from(
		com.aivle.project.company.reportanalysis.entity.ReportContentEntity entity
	) {
		return new ReportContentDto(
			entity.getId(),
			entity.getTitle(),
			entity.getSummary(),
			entity.getScore() != null ? entity.getScore().doubleValue() : null,
			entity.getPublishedAt() != null
				? entity.getPublishedAt().atZone(ZoneOffset.UTC).toOffsetDateTime()
				: null,
			entity.getLink(),
			entity.getSentiment()
		);
	}
}
