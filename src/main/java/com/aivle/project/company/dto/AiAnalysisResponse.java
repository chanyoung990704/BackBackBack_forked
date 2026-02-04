package com.aivle.project.company.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.Map;

/**
 * AI 서버 재무 예측 응답 DTO.
 * <p>
 * AI 서버가 snake_case로 내려주는 JSON을 record 필드로 매핑합니다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AiAnalysisResponse(
    String companyCode,
    String companyName,
    String basePeriod,
    Map<String, Double> predictions
) {
}

