package com.aivle.project.company.service;

import com.aivle.project.company.dto.CompanySectorDto;
import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.repository.CompaniesRepository;
import com.aivle.project.industry.entity.IndustryEntity;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기업 섹터 조회 서비스.
 */
@Service
@RequiredArgsConstructor
public class CompanySectorService {

	private final CompaniesRepository companiesRepository;

	/**
	 * 회사 ID 기준 섹터 정보를 조회한다.
	 */
	@Transactional(readOnly = true)
	public CompanySectorDto getSector(Long companyId) {
		CompaniesEntity company = companiesRepository.findById(companyId)
			.orElseThrow(() -> new IllegalArgumentException("Company not found for id: " + companyId));

		String label = Optional.ofNullable(company.getIndustryCode())
			.map(IndustryEntity::getIndustryName)
			.orElse("");

		return new CompanySectorDto("", label);
	}
}
