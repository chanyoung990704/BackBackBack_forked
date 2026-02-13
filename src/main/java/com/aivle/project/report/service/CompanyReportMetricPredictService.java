package com.aivle.project.report.service;

import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.repository.CompaniesRepository;
import com.aivle.project.common.util.GetOrCreateResolver;
import com.aivle.project.metric.entity.MetricValueType;
import com.aivle.project.metric.entity.MetricsEntity;
import com.aivle.project.metric.repository.MetricsRepository;
import com.aivle.project.quarter.entity.QuartersEntity;
import com.aivle.project.quarter.repository.QuartersRepository;
import com.aivle.project.quarter.support.QuarterCalculator;
import com.aivle.project.quarter.support.YearQuarter;
import com.aivle.project.report.dto.ReportPredictRequest;
import com.aivle.project.report.dto.ReportPredictResult;
import com.aivle.project.report.entity.CompanyReportMetricValuesEntity;
import com.aivle.project.report.entity.CompanyReportVersionsEntity;
import com.aivle.project.report.entity.CompanyReportsEntity;
import com.aivle.project.report.repository.CompanyReportMetricValuesRepository;
import com.aivle.project.report.repository.CompanyReportVersionsRepository;
import com.aivle.project.report.repository.CompanyReportsRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기업 보고서 지표 예측값 적재 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyReportMetricPredictService {

	private final CompaniesRepository companiesRepository;
	private final QuartersRepository quartersRepository;
	private final MetricsRepository metricsRepository;
	private final CompanyReportsRepository companyReportsRepository;
	private final CompanyReportVersionsRepository companyReportVersionsRepository;
	private final CompanyReportMetricValuesRepository companyReportMetricValuesRepository;
	private final CompanyReportVersionIssueService companyReportVersionIssueService;

	@Transactional
	public ReportPredictResult importPredictedMetrics(ReportPredictRequest request) {
		if (request == null || request.metrics() == null || request.metrics().isEmpty()) {
			return ReportPredictResult.empty();
		}

		String stockCode = normalizeStockCode(request.stockCode());
		if (stockCode.isBlank()) {
			log.info("예측값 적재 스킵: 기업 코드 누락");
			return new ReportPredictResult(request.metrics().size(), 0, 0, 1, null);
		}

		Optional<CompaniesEntity> company = companiesRepository.findByStockCode(stockCode);
		if (company.isEmpty()) {
			log.info("예측값 적재 스킵: 기업 코드 미존재 (stockCode={})", stockCode);
			return new ReportPredictResult(request.metrics().size(), 0, 0, 1, null);
		}

		int quarterKey = request.quarterKey();
		YearQuarter baseQuarter = QuarterCalculator.parseQuarterKey(quarterKey);
		QuartersEntity quarter = getOrCreateQuarter(quarterKey, baseQuarter);

		CompanyReportsEntity report = getOrCreateReport(company.get(), quarter);

		CompanyReportVersionsEntity version = resolveMetricVersion(report);

		List<String> metricNameEns = request.metrics().keySet().stream()
			.map(this::normalizeMetricNameEn)
			.filter(name -> !name.isBlank())
			.toList();
		Map<String, MetricsEntity> metricMap = metricsRepository.findAllByMetricNameEnIn(metricNameEns).stream()
			.collect(Collectors.toMap(MetricsEntity::getMetricNameEn, metric -> metric));

		int skippedMetrics = 0;
		List<CompanyReportMetricValuesEntity> values = new ArrayList<>();
		for (Map.Entry<String, java.math.BigDecimal> entry : request.metrics().entrySet()) {
			String metricNameEn = normalizeMetricNameEn(entry.getKey());
			if (metricNameEn.isBlank()) {
				skippedMetrics++;
				log.info("예측값 적재 스킵: metric_name_en 누락 (stockCode={})", stockCode);
				continue;
			}

			MetricsEntity metric = metricMap.get(metricNameEn);
			if (metric == null) {
				skippedMetrics++;
				log.info(
					"예측값 적재 스킵: metric_name_en 미존재 (metricNameEn={}, stockCode={})",
					metricNameEn,
					stockCode
				);
				continue;
			}

			values.add(CompanyReportMetricValuesEntity.create(
				version,
				metric,
				quarter,
				entry.getValue(),
				MetricValueType.PREDICTED
			));
		}

		companyReportMetricValuesRepository.saveAll(values);
		log.info(
			"예측값 적재 완료: stockCode={}, quarterKey={}, total={}, saved={}, skippedMetrics={}",
			stockCode,
			quarterKey,
			request.metrics().size(),
			values.size(),
			skippedMetrics
		);
		return new ReportPredictResult(request.metrics().size(), values.size(), skippedMetrics, 0, version.getVersionNo());
	}

	private CompanyReportVersionsEntity resolveMetricVersion(CompanyReportsEntity report) {
		return companyReportVersionsRepository.findTopByCompanyReportAndPublishedFalseOrderByVersionNoDesc(report)
			.filter(existing -> !companyReportMetricValuesRepository
				.existsByReportVersionAndValueTypeAndMetricValueIsNotNull(existing, MetricValueType.PREDICTED))
			.orElseGet(() -> createNewVersion(report));
	}

	private CompanyReportVersionsEntity createNewVersion(CompanyReportsEntity report) {
		return companyReportVersionIssueService.issueNextVersion(report, false, null);
	}

	private CompanyReportsEntity getOrCreateReport(CompaniesEntity company, QuartersEntity quarter) {
		return GetOrCreateResolver.resolve(
			() -> companyReportsRepository.findByCompanyIdAndQuarterId(company.getId(), quarter.getId()),
			() -> companyReportsRepository.save(CompanyReportsEntity.create(company, quarter, null)),
			() -> companyReportsRepository.findByCompanyIdAndQuarterId(company.getId(), quarter.getId())
		);
	}

	private QuartersEntity getOrCreateQuarter(int quarterKey, YearQuarter yearQuarter) {
		return GetOrCreateResolver.resolve(
			() -> quartersRepository.findByQuarterKey(quarterKey),
			() -> quartersRepository.save(QuartersEntity.create(
				yearQuarter.year(),
				yearQuarter.quarter(),
				quarterKey,
				QuarterCalculator.startDate(yearQuarter),
				QuarterCalculator.endDate(yearQuarter)
			)),
			() -> quartersRepository.findByQuarterKey(quarterKey)
		);
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
