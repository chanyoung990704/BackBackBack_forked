package com.aivle.project.company.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 핵심 지표 DTO.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CompanyOverviewKeyMetricDto {
	private String key;
	private String label;
	private Double value;
	private String unit;
	private CompanyOverviewTooltipDto tooltip;
}
