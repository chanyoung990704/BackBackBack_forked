package com.aivle.project.report.dto;

import com.aivle.project.metric.entity.MetricValueType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Map;

/**
 * 보고서 지표 수동 적재 요청 DTO.
 */
public record ReportMetricPublishRequest(
	@NotBlank(message = "기업 코드는 필수입니다.")
	String stockCode,
	@NotNull(message = "quarterKey는 필수입니다.")
	Integer quarterKey,
	@NotNull(message = "valueType은 필수입니다.")
	MetricValueType valueType,
	@NotNull(message = "metrics는 필수입니다.")
	Map<String, BigDecimal> metrics
) {
}
