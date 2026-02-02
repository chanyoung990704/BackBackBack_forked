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
import com.aivle.project.metric.entity.MetricValueType;
import com.aivle.project.metric.entity.MetricsEntity;
import com.aivle.project.metric.repository.MetricsRepository;
import com.aivle.project.quarter.entity.QuartersEntity;
import com.aivle.project.quarter.repository.QuartersRepository;
import com.aivle.project.quarter.support.QuarterCalculator;
import com.aivle.project.quarter.support.YearQuarter;
import com.aivle.project.report.dto.ReportPublishResult;
import com.aivle.project.report.entity.CompanyReportMetricValuesEntity;
import com.aivle.project.report.entity.CompanyReportVersionsEntity;
import com.aivle.project.report.entity.CompanyReportsEntity;
import com.aivle.project.report.repository.CompanyReportMetricValuesRepository;
import com.aivle.project.report.repository.CompanyReportVersionsRepository;
import com.aivle.project.report.repository.CompanyReportsRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 보고서 지표 적재 및 PDF 발행 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyReportMetricPublishService {

	private final CompaniesRepository companiesRepository;
	private final QuartersRepository quartersRepository;
	private final MetricsRepository metricsRepository;
	private final CompanyReportsRepository companyReportsRepository;
	private final CompanyReportVersionsRepository companyReportVersionsRepository;
	private final CompanyReportMetricValuesRepository companyReportMetricValuesRepository;
	private final FileStorageService fileStorageService;
	private final FileValidator fileValidator;
	private final FilesRepository filesRepository;

	@Transactional
	public ReportPublishResult publishMetrics(
		String stockCode,
		int quarterKey,
		MetricValueType valueType,
		Map<String, BigDecimal> metrics,
		MultipartFile pdfFile
	) {
		if (metrics == null || metrics.isEmpty()) {
			return ReportPublishResult.empty();
		}

		String normalizedStockCode = normalizeStockCode(stockCode);
		if (normalizedStockCode.isBlank()) {
			log.info("보고서 발행 스킵: 기업 코드 누락");
			return new ReportPublishResult(metrics.size(), 0, 0, 1, null, null);
		}

		Optional<CompaniesEntity> company = companiesRepository.findByStockCode(normalizedStockCode);
		if (company.isEmpty()) {
			log.info("보고서 발행 스킵: 기업 코드 미존재 (stockCode={})", normalizedStockCode);
			return new ReportPublishResult(metrics.size(), 0, 0, 1, null, null);
		}

		validatePdf(pdfFile);

		YearQuarter baseQuarter = QuarterCalculator.parseQuarterKey(quarterKey);
		QuartersEntity quarter = getOrCreateQuarter(quarterKey, baseQuarter);
		CompanyReportsEntity report = companyReportsRepository.findByCompanyIdAndQuarterId(
			company.get().getId(),
			quarter.getId()
		).orElseGet(() -> companyReportsRepository.save(CompanyReportsEntity.create(company.get(), quarter, null)));

		CompanyReportVersionsEntity version = createNewVersion(report);

		Map<String, MetricsEntity> metricMap = metricsRepository.findAllByMetricNameEnIn(
				metrics.keySet().stream()
					.map(this::normalizeMetricNameEn)
					.filter(name -> !name.isBlank())
					.toList()
			).stream()
			.collect(Collectors.toMap(MetricsEntity::getMetricNameEn, metric -> metric));

		List<CompanyReportMetricValuesEntity> values = new ArrayList<>();
		int skippedMetrics = 0;
		for (Map.Entry<String, BigDecimal> entry : metrics.entrySet()) {
			String metricNameEn = normalizeMetricNameEn(entry.getKey());
			if (metricNameEn.isBlank()) {
				skippedMetrics++;
				log.info("보고서 발행 스킵: metric_name_en 누락 (stockCode={})", normalizedStockCode);
				continue;
			}

			MetricsEntity metric = metricMap.get(metricNameEn);
			if (metric == null) {
				skippedMetrics++;
				log.info(
					"보고서 발행 스킵: metric_name_en 미존재 (metricNameEn={}, stockCode={})",
					metricNameEn,
					normalizedStockCode
				);
				continue;
			}

			values.add(CompanyReportMetricValuesEntity.create(
				version,
				metric,
				quarter,
				entry.getValue(),
				valueType
			));
		}

		companyReportMetricValuesRepository.saveAll(values);

		FilesEntity pdfEntity = savePdf(report, version, pdfFile);
		version.publishWithPdf(pdfEntity);
		companyReportVersionsRepository.save(version);

		log.info(
			"보고서 발행 완료: stockCode={}, quarterKey={}, valueType={}, total={}, saved={}, skippedMetrics={}, versionNo={}",
			normalizedStockCode,
			quarterKey,
			valueType,
			metrics.size(),
			values.size(),
			skippedMetrics,
			version.getVersionNo()
		);
		return new ReportPublishResult(
			metrics.size(),
			values.size(),
			skippedMetrics,
			0,
			version.getVersionNo(),
			pdfEntity.getId()
		);
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

	private String normalizeMetricNameEn(String metricNameEn) {
		if (metricNameEn == null) {
			return "";
		}
		return metricNameEn.trim();
	}
}
