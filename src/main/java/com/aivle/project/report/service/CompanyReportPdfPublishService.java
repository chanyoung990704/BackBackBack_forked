package com.aivle.project.report.service;

import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.repository.CompaniesRepository;
import com.aivle.project.file.entity.FileUsageType;
import com.aivle.project.file.entity.FilesEntity;
import com.aivle.project.file.exception.FileErrorCode;
import com.aivle.project.file.exception.FileException;
import com.aivle.project.file.repository.FilesRepository;
import com.aivle.project.file.storage.FileStorageService;
import com.aivle.project.file.storage.StoredFile;
import com.aivle.project.file.validator.FileValidator;
import com.aivle.project.quarter.entity.QuartersEntity;
import com.aivle.project.quarter.repository.QuartersRepository;
import com.aivle.project.quarter.support.QuarterCalculator;
import com.aivle.project.quarter.support.YearQuarter;
import com.aivle.project.report.dto.ReportPdfPublishResult;
import com.aivle.project.report.entity.CompanyReportVersionsEntity;
import com.aivle.project.report.entity.CompanyReportsEntity;
import com.aivle.project.report.repository.CompanyReportVersionsRepository;
import com.aivle.project.report.repository.CompanyReportsRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 보고서 PDF 단일 업로드 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyReportPdfPublishService {

	private final CompaniesRepository companiesRepository;
	private final QuartersRepository quartersRepository;
	private final CompanyReportsRepository companyReportsRepository;
	private final CompanyReportVersionsRepository companyReportVersionsRepository;
	private final FileStorageService fileStorageService;
	private final FileValidator fileValidator;
	private final FilesRepository filesRepository;

	@Transactional
	public ReportPdfPublishResult publishPdfOnly(String stockCode, int quarterKey, MultipartFile pdfFile) {
		String normalizedStockCode = normalizeStockCode(stockCode);
		if (normalizedStockCode.isBlank()) {
			log.info("PDF 업로드 스킵: 기업 코드 누락");
			return new ReportPdfPublishResult(0, 1, null, null);
		}

		Optional<CompaniesEntity> company = companiesRepository.findByStockCode(normalizedStockCode);
		if (company.isEmpty()) {
			log.info("PDF 업로드 스킵: 기업 코드 미존재 (stockCode={})", normalizedStockCode);
			return new ReportPdfPublishResult(0, 1, null, null);
		}

		validatePdf(pdfFile);

		YearQuarter baseQuarter = QuarterCalculator.parseQuarterKey(quarterKey);
		QuartersEntity quarter = getOrCreateQuarter(quarterKey, baseQuarter);

		CompanyReportsEntity report = companyReportsRepository.findByCompanyIdAndQuarterId(
			company.get().getId(),
			quarter.getId()
		).orElseGet(() -> companyReportsRepository.save(CompanyReportsEntity.create(company.get(), quarter, null)));

		CompanyReportVersionsEntity version = createNewVersion(report);
		FilesEntity pdfEntity = savePdf(report, version, pdfFile);
		version.publishWithPdf(pdfEntity);
		companyReportVersionsRepository.save(version);

		log.info(
			"PDF 업로드 완료: stockCode={}, quarterKey={}, versionNo={}",
			normalizedStockCode,
			quarterKey,
			version.getVersionNo()
		);
		return new ReportPdfPublishResult(1, 0, version.getVersionNo(), pdfEntity.getId());
	}

	private CompanyReportVersionsEntity createNewVersion(CompanyReportsEntity report) {
		int nextVersion = companyReportVersionsRepository.findTopByCompanyReportOrderByVersionNoDesc(report)
			.map(existing -> existing.getVersionNo() + 1)
			.orElse(1);
		CompanyReportVersionsEntity version = CompanyReportVersionsEntity.create(
			report,
			nextVersion,
			LocalDateTime.now(),
			false,
			null
		);
		return companyReportVersionsRepository.save(version);
	}

	private QuartersEntity getOrCreateQuarter(int quarterKey, YearQuarter yearQuarter) {
		return quartersRepository.findByQuarterKey(quarterKey)
			.orElseGet(() -> quartersRepository.save(QuartersEntity.create(
				yearQuarter.year(),
				yearQuarter.quarter(),
				quarterKey,
				QuarterCalculator.startDate(yearQuarter),
				QuarterCalculator.endDate(yearQuarter)
			)));
	}

	private FilesEntity savePdf(CompanyReportsEntity report, CompanyReportVersionsEntity version, MultipartFile file) {
		String keyPrefix = "reports/" + report.getId() + "/v" + version.getVersionNo();
		StoredFile stored = fileStorageService.store(file, keyPrefix);
		FilesEntity entity = FilesEntity.create(
			FileUsageType.REPORT_PDF,
			stored.storageUrl(),
			stored.storageKey(),
			stored.originalFilename(),
			stored.fileSize(),
			stored.contentType()
		);
		return filesRepository.save(entity);
	}

	private void validatePdf(MultipartFile file) {
		fileValidator.validate(file);
		String contentType = file.getContentType();
		if (contentType == null || !"application/pdf".equalsIgnoreCase(contentType)) {
			throw new FileException(FileErrorCode.FILE_400_CONTENT_TYPE);
		}
		String filename = file.getOriginalFilename();
		if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
			throw new FileException(FileErrorCode.FILE_400_EXTENSION);
		}
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
