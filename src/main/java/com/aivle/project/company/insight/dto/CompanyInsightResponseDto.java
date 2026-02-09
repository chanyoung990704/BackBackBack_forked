package com.aivle.project.company.insight.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 기업 인사이트 응답 DTO.
 */
@Getter
@Builder
public class CompanyInsightResponseDto {

	private BigDecimal averageScore;
	private List<CompanyInsightDto> items;
}
