package com.aivle.project.company.news.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

/**
 * AI 서버 뉴스 분석 API 응답 DTO.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record NewsApiResponse(
    String companyName,
    Integer totalCount,
    List<NewsItemResponse> news,
    Double averageScore,
    String analyzedAt
) {
}
