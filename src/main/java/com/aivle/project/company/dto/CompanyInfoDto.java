package com.aivle.project.company.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 기업 기본 정보 DTO.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CompanyInfoDto {
	private Long id;
	private String name;
	private String stockCode;
	private CompanySectorDto sector;
	private Double overallScore;
	private String riskLevel;
	private Double networkHealth;
	private Double reputationScore;
}
