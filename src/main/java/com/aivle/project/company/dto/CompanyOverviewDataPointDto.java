package com.aivle.project.company.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 분기별 지표 값 DTO.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CompanyOverviewDataPointDto {
	private String quarter;
	private Double value;
	private CompanyOverviewDataType type;
}
