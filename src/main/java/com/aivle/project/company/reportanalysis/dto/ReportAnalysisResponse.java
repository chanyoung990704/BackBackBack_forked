package com.aivle.project.company.reportanalysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 사업보고서 분석 조회 응답 DTO.
 */
@Schema(description = "사업보고서 분석 조회 응답")
public record ReportAnalysisResponse(
	@Schema(description = "분석 ID", example = "1")
	Long id,

	@Schema(description = "기업 ID", example = "100")
	Long companyId,

	@Schema(description = "기업명", example = "동화약품")
	String companyName,

	@Schema(description = "분석된 항목 수", example = "1")
	Integer totalCount,

	@Schema(description = "평균 점수", example = "0.0041")
	Double averageScore,

	@Schema(description = "AI 분석 시각 (UTC)", example = "2026-02-05T12:35:41.369155Z")
	OffsetDateTime analyzedAt,

	@Schema(description = "요약 항목 목록")
	List<ReportContentDto> contents,

	@Schema(description = "생성 시각 (UTC)", example = "2026-02-05T12:35:41.369155Z")
	OffsetDateTime createdAt
) {

	public static ReportAnalysisResponse from(
		Long companyId,
		com.aivle.project.company.reportanalysis.entity.ReportAnalysisEntity entity,
		List<com.aivle.project.company.reportanalysis.entity.ReportContentEntity> contents
	) {
		return new ReportAnalysisResponse(
			entity.getId(),
			companyId,
			entity.getCompanyName(),
			entity.getTotalCount(),
			entity.getAverageScore() != null ? entity.getAverageScore().doubleValue() : null,
			entity.getAnalyzedAt().atZone(ZoneOffset.UTC).toOffsetDateTime(),
			contents.stream()
				.map(ReportContentDto::from)
				.collect(Collectors.toList()),
			entity.getCreatedAt().atZone(ZoneOffset.UTC).toOffsetDateTime()
		);
	}
}
