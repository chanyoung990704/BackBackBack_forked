package com.aivle.project.company.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 지표 시계열 DTO.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CompanyOverviewMetricSeriesDto {
	private String key;
	private String label;
	private String unit;
	private List<CompanyOverviewDataPointDto> points;
}
