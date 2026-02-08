package com.aivle.project.company.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 엑셀에서 추출한 기업 업종 정보 DTO.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CompanyIndustryImportDto {
    private String stockCode;    // 기업코드 (6자리)
    private String corpName;     // 기업명
    private String industryCode; // 업종코드
    private String industryName; // 업종명
}
