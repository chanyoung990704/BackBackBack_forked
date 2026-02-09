package com.aivle.project.company.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

/**
 * AI 서버 재무건전성 점수 응답 DTO.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AiHealthScoreResponse(
	String companyCode,
	String companyName,
	List<HealthScoreQuarter> quarters,
	Integer currentScore,
	Integer predictedScore
) {
	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
	public record HealthScoreQuarter(
		String period,
		Double score,
		String label,
		String type
	) {
	}
}
