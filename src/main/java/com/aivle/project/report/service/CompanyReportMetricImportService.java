package com.aivle.project.report.service;

import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.repository.CompaniesRepository;
import com.aivle.project.metric.entity.MetricValueType;
import com.aivle.project.metric.entity.MetricsEntity;
import com.aivle.project.metric.repository.MetricsRepository;
import com.aivle.project.quarter.entity.QuartersEntity;
import com.aivle.project.quarter.repository.QuartersRepository;
import com.aivle.project.quarter.support.QuarterCalculator;
import com.aivle.project.quarter.support.YearQuarter;
import com.aivle.project.report.dto.CompanyMetricValueCommand;
import com.aivle.project.report.dto.ReportImportResult;
import com.aivle.project.report.entity.CompanyReportMetricValuesEntity;
import com.aivle.project.report.entity.CompanyReportVersionsEntity;
import com.aivle.project.report.entity.CompanyReportsEntity;
import com.aivle.project.report.repository.CompanyReportMetricValuesRepository;
import com.aivle.project.report.repository.CompanyReportVersionsRepository;
import com.aivle.project.report.repository.CompanyReportsRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기업 보고서 지표 적재 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyReportMetricImportService {

	private final CompaniesRepository companiesRepository;
	private final QuartersRepository quartersRepository;
	private final MetricsRepository metricsRepository;
	private final CompanyReportsRepository companyReportsRepository;
	private final CompanyReportVersionsRepository companyReportVersionsRepository;
	private final CompanyReportMetricValuesRepository companyReportMetricValuesRepository;

	@Transactional
	public ReportImportResult importMetrics(int baseQuarterKey, List<CompanyMetricValueCommand> commands) {
		if (commands == null || commands.isEmpty()) {
			return ReportImportResult.empty();
		}

		log.info("지표 적재 시작: baseQuarterKey={}, commands={}", baseQuarterKey, commands.size());
		YearQuarter baseQuarter = QuarterCalculator.parseQuarterKey(baseQuarterKey);
		QuartersEntity baseQuarterEntity = getOrCreateQuarter(baseQuarterKey, baseQuarter);

		Map<String, List<CompanyMetricValueCommand>> commandsByCompany = groupByCompany(commands);
		Map<String, MetricsEntity> metricCache = new HashMap<>();
		Map<Integer, QuartersEntity> quarterCache = new HashMap<>();
		quarterCache.put(baseQuarterKey, baseQuarterEntity);

		int savedValues = 0;
		int skippedCompanies = 0;
		int skippedMetrics = 0;

		for (Map.Entry<String, List<CompanyMetricValueCommand>> entry : commandsByCompany.entrySet()) {
			String stockCode = entry.getKey();
			Optional<CompaniesEntity> company = companiesRepository.findByStockCode(stockCode);
			if (company.isEmpty()) {
				skippedCompanies++;
				log.info("지표 적재 스킵: 기업 코드 미존재 (stockCode={})", stockCode);
				continue;
			}

			Map<Long, List<MetricValueSeed>> seedsByQuarter = new LinkedHashMap<>();
			for (CompanyMetricValueCommand command : entry.getValue()) {
				String metricCode = normalizeMetricCode(command.metricCode());
				if (metricCode.isBlank()) {
					skippedMetrics++;
					log.info(
						"지표 적재 스킵: metric_code 누락 (stockCode={}, row={}, col={}, header={})",
						stockCode,
						command.rowIndex(),
						command.colIndex(),
						command.headerName()
					);
					continue;
				}

				MetricsEntity metric = metricCache.computeIfAbsent(metricCode, this::findMetric);
				if (metric == null) {
					skippedMetrics++;
					log.info(
						"지표 적재 스킵: 지표 코드 미존재 (metricCode={}, stockCode={}, row={}, col={}, header={})",
						metricCode,
						stockCode,
						command.rowIndex(),
						command.colIndex(),
						command.headerName()
					);
					continue;
				}

				int quarterKey = toQuarterKey(baseQuarter, command.quarterOffset());
				QuartersEntity quarter = quarterCache.computeIfAbsent(
					quarterKey,
					key -> getOrCreateQuarter(key, QuarterCalculator.parseQuarterKey(key))
				);

				seedsByQuarter.computeIfAbsent(quarter.getId(), ignored -> new ArrayList<>()).add(new MetricValueSeed(
					metric,
					quarter,
					command.metricValue(),
					command.rowIndex(),
					command.colIndex(),
					command.headerName(),
					command.quarterOffset()
				));
			}

			if (seedsByQuarter.isEmpty()) {
				log.info("지표 적재 스킵: 유효 지표 없음 (stockCode={})", stockCode);
				continue;
			}
			for (Map.Entry<Long, List<MetricValueSeed>> quarterEntry : seedsByQuarter.entrySet()) {
				QuartersEntity quarter = quarterEntry.getValue().get(0).quarter();
				CompanyReportsEntity report = getOrCreateReport(company.get(), quarter);
				CompanyReportVersionsEntity version = createNewVersion(report);

				List<CompanyReportMetricValuesEntity> values = new ArrayList<>();
				Map<MetricKey, MetricValueSeed> duplicateCheck = new HashMap<>();
				int duplicates = 0;
				for (MetricValueSeed seed : quarterEntry.getValue()) {
					MetricKey key = new MetricKey(seed.metric().getId(), MetricValueType.ACTUAL);
					MetricValueSeed existing = duplicateCheck.putIfAbsent(key, seed);
					if (existing != null) {
						duplicates++;
						log.info(
							"지표 중복 감지: stockCode={}, reportVersionId={}, metricId={}, quarterId={}, valueType={}, " +
								"existing(row={}, col={}, header={}, offset={}), duplicate(row={}, col={}, header={}, offset={})",
							stockCode,
							version.getId(),
							seed.metric().getId(),
							seed.quarter().getId(),
							MetricValueType.ACTUAL,
							existing.rowIndex(),
							existing.colIndex(),
							existing.headerName(),
							existing.quarterOffset(),
							seed.rowIndex(),
							seed.colIndex(),
							seed.headerName(),
							seed.quarterOffset()
						);
					}
					values.add(CompanyReportMetricValuesEntity.create(
						version,
						seed.metric(),
						seed.quarter(),
						seed.metricValue(),
						MetricValueType.ACTUAL
					));
				}

				companyReportMetricValuesRepository.saveAll(values);
				savedValues += values.size();
				if (duplicates > 0) {
					log.info("지표 적재 중복 요약: stockCode={}, quarterKey={}, duplicates={}", stockCode, quarter.getQuarterKey(), duplicates);
				}
			}
		}

		log.info(
			"지표 적재 완료: baseQuarterKey={}, total={}, saved={}, skippedCompanies={}, skippedMetrics={}",
			baseQuarterKey,
			commands.size(),
			savedValues,
			skippedCompanies,
			skippedMetrics
		);
		return new ReportImportResult(commands.size(), savedValues, skippedCompanies, skippedMetrics);
	}

	private Map<String, List<CompanyMetricValueCommand>> groupByCompany(List<CompanyMetricValueCommand> commands) {
		Map<String, List<CompanyMetricValueCommand>> grouped = new LinkedHashMap<>();
		for (CompanyMetricValueCommand command : commands) {
			if (command == null) {
				continue;
			}
			String stockCode = normalizeStockCode(command.stockCode());
			if (stockCode.isBlank()) {
				log.info(
					"지표 적재 스킵: 기업 코드 누락 (metricCode={}, row={}, col={}, header={})",
					command.metricCode(),
					command.rowIndex(),
					command.colIndex(),
					command.headerName()
				);
				continue;
			}
			grouped.computeIfAbsent(stockCode, key -> new ArrayList<>()).add(command);
		}
		return grouped;
	}

	private MetricsEntity findMetric(String metricCode) {
		return metricsRepository.findByMetricCode(metricCode).orElse(null);
	}

	private CompanyReportsEntity getOrCreateReport(CompaniesEntity company, QuartersEntity quarter) {
		return companyReportsRepository.findByCompanyIdAndQuarterId(company.getId(), quarter.getId())
			.orElseGet(() -> companyReportsRepository.save(CompanyReportsEntity.create(company, quarter, null)));
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

	private int toQuarterKey(YearQuarter baseQuarter, int offset) {
		YearQuarter targetQuarter = QuarterCalculator.offset(baseQuarter, offset);
		return targetQuarter.year() * 10 + targetQuarter.quarter();
	}

	private String normalizeMetricCode(String metricCode) {
		if (metricCode == null) {
			return "";
		}
		return metricCode.trim();
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

	private record MetricValueSeed(
		MetricsEntity metric,
		QuartersEntity quarter,
		java.math.BigDecimal metricValue,
		int rowIndex,
		int colIndex,
		String headerName,
		int quarterOffset
	) {
	}

	private record MetricKey(Long metricId, MetricValueType valueType) {
	}
}
