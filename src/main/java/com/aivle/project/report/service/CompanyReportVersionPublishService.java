package com.aivle.project.report.service;

import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.repository.CompaniesRepository;
import com.aivle.project.quarter.entity.QuartersEntity;
import com.aivle.project.quarter.repository.QuartersRepository;
import com.aivle.project.report.dto.ReportVersionPublishResult;
import com.aivle.project.report.entity.CompanyReportVersionsEntity;
import com.aivle.project.report.entity.CompanyReportsEntity;
import com.aivle.project.report.repository.CompanyReportVersionsRepository;
import com.aivle.project.report.repository.CompanyReportsRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 보고서 버전 발행 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyReportVersionPublishService {

	private final CompaniesRepository companiesRepository;
	private final QuartersRepository quartersRepository;
	private final CompanyReportsRepository companyReportsRepository;
	private final CompanyReportVersionsRepository companyReportVersionsRepository;

	@Transactional
	public ReportVersionPublishResult publishLatestVersion(String stockCode, int quarterKey) {
		String normalizedStockCode = normalizeStockCode(stockCode);
		if (normalizedStockCode.isBlank()) {
			log.info("보고서 발행 스킵: 기업 코드 누락");
			return new ReportVersionPublishResult(0, 1, 0, 0, null);
		}

		Optional<CompaniesEntity> company = companiesRepository.findByStockCode(normalizedStockCode);
		if (company.isEmpty()) {
			log.info("보고서 발행 스킵: 기업 코드 미존재 (stockCode={})", normalizedStockCode);
			return new ReportVersionPublishResult(0, 1, 0, 0, null);
		}

		Optional<QuartersEntity> quarter = quartersRepository.findByQuarterKey(quarterKey);
		if (quarter.isEmpty()) {
			log.info(
				"보고서 발행 스킵: 분기 미존재 (stockCode={}, quarterKey={})",
				normalizedStockCode,
				quarterKey
			);
			return new ReportVersionPublishResult(0, 0, 1, 0, null);
		}

		Optional<CompanyReportsEntity> report = companyReportsRepository.findByCompanyIdAndQuarterId(
			company.get().getId(),
			quarter.get().getId()
		);
		if (report.isEmpty()) {
			log.info(
				"보고서 발행 스킵: 보고서 미존재 (stockCode={}, quarterKey={})",
				normalizedStockCode,
				quarterKey
			);
			return new ReportVersionPublishResult(0, 0, 1, 0, null);
		}

		Optional<CompanyReportVersionsEntity> version =
			companyReportVersionsRepository.findTopByCompanyReportOrderByVersionNoDesc(report.get());
		if (version.isEmpty()) {
			log.info(
				"보고서 발행 스킵: 보고서 버전 미존재 (stockCode={}, quarterKey={})",
				normalizedStockCode,
				quarterKey
			);
			return new ReportVersionPublishResult(0, 0, 0, 1, null);
		}

		CompanyReportVersionsEntity latestVersion = version.get();
		if (latestVersion.isPublished()) {
			return new ReportVersionPublishResult(0, 0, 0, 1, latestVersion.getVersionNo());
		}

		latestVersion.publish();
		companyReportVersionsRepository.save(latestVersion);
		return new ReportVersionPublishResult(1, 0, 0, 0, latestVersion.getVersionNo());
	}

	private String normalizeStockCode(String stockCode) {
		if (stockCode == null) {
			return "";
		}
		String trimmed = stockCode.trim();
		if (trimmed.isBlank()) {
			return "";
		}
		if (trimmed.length() < 6) {
			return "0".repeat(6 - trimmed.length()) + trimmed;
		}
		return trimmed;
	}
}
