package com.aivle.project.company.service;

import com.aivle.project.company.dto.CompanyInfoDto;
import com.aivle.project.company.dto.CompanySectorDto;
import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.keymetric.entity.CompanyKeyMetricEntity;
import com.aivle.project.company.keymetric.repository.CompanyKeyMetricRepository;
import com.aivle.project.company.repository.CompaniesRepository;
import com.aivle.project.quarter.entity.QuartersEntity;
import com.aivle.project.quarter.repository.QuartersRepository;
import com.aivle.project.quarter.support.QuarterCalculator;
import com.aivle.project.quarter.support.YearQuarter;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 기업 기본 정보 조회 서비스.
 */
@Service
@RequiredArgsConstructor
public class CompanyInfoService {

	private final CompaniesRepository companiesRepository;
	private final QuartersRepository quartersRepository;
	private final CompanyKeyMetricRepository companyKeyMetricRepository;
	private final CompanySectorService companySectorService;

	/**
	 * 기업 기본 정보를 분기 기준으로 조회한다.
	 */
	public CompanyInfoDto getCompanyInfo(Long companyId, String quarterKey) {
		CompaniesEntity company = companiesRepository.findById(companyId)
			.orElseThrow(() -> new IllegalArgumentException("Company not found for id: " + companyId));

		int parsedQuarterKey = parseQuarterKey(quarterKey);
		QuartersEntity quarter = quartersRepository.findByQuarterKey(parsedQuarterKey)
			.orElseThrow(() -> new IllegalArgumentException("Quarter not found for key: " + parsedQuarterKey));

		CompanySectorDto sector = companySectorService.getSector(companyId);
		CompanyKeyMetricEntity keyMetric = companyKeyMetricRepository
			.findByCompanyIdAndQuarterId(companyId, quarter.getId())
			.orElse(null);

		Double networkHealth = toDouble(keyMetric != null ? keyMetric.getInternalHealthScore() : null);
		Double reputationScore = toDouble(keyMetric != null ? keyMetric.getExternalHealthScore() : null);
		String riskLevel = keyMetric != null && keyMetric.getRiskLevel() != null
			? keyMetric.getRiskLevel().name()
			: null;

		return new CompanyInfoDto(
			company.getId(),
			company.getCorpName(),
			company.getStockCode(),
			sector,
			networkHealth,
			riskLevel,
			networkHealth,
			reputationScore
		);
	}

	private int parseQuarterKey(String quarterKey) {
		try {
			String normalized = normalizeQuarterKey(quarterKey);
			int parsed = Integer.parseInt(normalized);
			YearQuarter yearQuarter = QuarterCalculator.parseQuarterKey(parsed);
			return yearQuarter.toQuarterKey();
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("quarterKey는 숫자 형식이어야 합니다.", e);
		}
	}

	private String normalizeQuarterKey(String quarterKey) {
		if (quarterKey == null) {
			return null;
		}
		String trimmed = quarterKey.trim();
		if (trimmed.length() == 6) {
			String yearPart = trimmed.substring(0, 4);
			String quarterPart = trimmed.substring(4, 6);
			if (quarterPart.startsWith("0")) {
				quarterPart = quarterPart.substring(1);
			}
			return yearPart + quarterPart;
		}
		return trimmed;
	}

	private Double toDouble(BigDecimal value) {
		return value == null ? null : value.doubleValue();
	}
}
