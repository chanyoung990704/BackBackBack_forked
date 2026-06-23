package com.aivle.project.company.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Map;

/**
 * 보고서 지표 예측값 요청 DTO.
 */
public record ReportPredictRequest(
	@NotBlank(message = "기업 코드는 필수입니다.")
	String stockCode,
	@NotNull(message = "quarterKey는 필수입니다.")
	Integer quarterKey,
	@NotNull(message = "metrics는 필수입니다.")
	Map<String, BigDecimal> metrics
) {
}
