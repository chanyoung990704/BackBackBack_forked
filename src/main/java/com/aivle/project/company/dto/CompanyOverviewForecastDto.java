package com.aivle.project.company.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 예측 요약 DTO.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CompanyOverviewForecastDto {
	private String latestActualQuarter;
	private String nextQuarter;
	private List<CompanyOverviewMetricSeriesDto> metricSeries;
}
