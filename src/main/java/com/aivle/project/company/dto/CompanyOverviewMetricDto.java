package com.aivle.project.company.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 개별 지표 DTO.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CompanyOverviewMetricDto {
	private String key;
	private String label;
	private CompanyOverviewSignalLevel level;
	private Double value;
	private String unit;
	private CompanyOverviewTooltipDto tooltip;
}
