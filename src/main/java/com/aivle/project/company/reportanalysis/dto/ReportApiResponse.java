package com.aivle.project.company.reportanalysis.dto;

import com.aivle.project.company.news.dto.NewsItemResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

/**
 * AI 서버 사업보고서 분석 API 응답 DTO.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ReportApiResponse(
	String companyName,
	Integer totalCount,
	List<NewsItemResponse> news,
	Double averageScore,
	String analyzedAt
) {
}
