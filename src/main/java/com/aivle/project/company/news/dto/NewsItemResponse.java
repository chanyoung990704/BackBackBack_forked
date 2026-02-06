package com.aivle.project.company.news.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
/**
 * AI 서버 뉴스 분석 응답의 news 배열 요소 DTO.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record NewsItemResponse(
    String title,
    String summary,
    Double score,
    String date,
    String link,
    String sentiment
) {
}
