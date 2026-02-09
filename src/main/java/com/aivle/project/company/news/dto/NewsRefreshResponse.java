package com.aivle.project.company.news.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 뉴스 재수집 및 평균 점수 복구 응답 DTO.
 */
@Schema(description = "뉴스 재수집 및 평균 점수 복구 응답")
public record NewsRefreshResponse(
	@Schema(description = "재수집된 최신 분석 결과")
	NewsAnalysisResponse analysis,

	@Schema(description = "average_score 복구 여부", example = "true")
	boolean averageScoreRepaired
) {
}
