package com.aivle.project.company.service;

import com.aivle.project.company.client.AiServerClient;
import com.aivle.project.company.dto.AiSignalResponse;
import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.repository.CompaniesRepository;
import com.aivle.project.metric.repository.MetricsRepository;
import com.aivle.project.report.entity.CompanyReportMetricValuesEntity;
import com.aivle.project.report.entity.SignalColor;
import com.aivle.project.report.repository.CompanyReportMetricValuesRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 업종 상대 신호등 캐시 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompanySignalCacheService {

	private final AiServerClient aiServerClient;
	private final CompaniesRepository companiesRepository;
	private final CompanyReportMetricValuesRepository companyReportMetricValuesRepository;
	private final MetricsRepository metricsRepository;

	/**
	 * 최신 ACTUAL 분기일 때만 신호등 캐시를 시도한다.
	 */
	@Transactional
	public void ensureSignalsCached(Long companyId, int requestedQuarterKey) {
		CompaniesEntity company = companiesRepository.findById(companyId)
			.orElseThrow(() -> new IllegalArgumentException("Company not found: " + companyId));

		Optional<Integer> latestActualQuarterKey = companyReportMetricValuesRepository
			.findMaxActualQuarterKeyByStockCode(company.getStockCode());
		if (latestActualQuarterKey.isEmpty()) {
			return;
		}
		int latestKey = latestActualQuarterKey.get();
		if (requestedQuarterKey != latestKey) {
			return;
		}

		List<CompanyReportMetricValuesEntity> latestActualValues = companyReportMetricValuesRepository
			.findLatestActualValuesByCompanyAndQuarter(companyId, latestKey);
		if (latestActualValues.isEmpty()) {
			return;
		}
		if (latestActualValues.stream().allMatch(value -> value.getSignalColor() != null)) {
			return;
		}

		AiSignalResponse response = aiServerClient.getSignals(company.getStockCode(), String.valueOf(latestKey));
		if (response == null || response.signals() == null || response.signals().isEmpty()) {
			log.warn("Empty AI signal response for company: {}", company.getStockCode());
			return;
		}

		Map<String, SignalColor> signalMap = mapSignals(response.signals());
		if (signalMap.isEmpty()) {
			return;
		}

		Set<String> metricCodes = signalMap.keySet();
		Map<Long, String> metricIdMap = metricsRepository.findAllByMetricCodeIn(metricCodes)
			.stream()
			.collect(Collectors.toMap(m -> m.getId(), m -> m.getMetricCode()));

		for (CompanyReportMetricValuesEntity value : latestActualValues) {
			Long metricId = value.getMetric().getId();
			String metricCode = metricIdMap.get(metricId);
			if (metricCode == null) {
				continue;
			}
			SignalColor color = signalMap.get(metricCode);
			if (color == null) {
				continue;
			}
			value.applySignal(color, null, (BigDecimal) null);
		}
	}

	private Map<String, SignalColor> mapSignals(Map<String, String> signals) {
		return signals.entrySet().stream()
			.filter(entry -> entry.getKey() != null && entry.getValue() != null)
			.map(entry -> new java.util.AbstractMap.SimpleEntry<>(
				entry.getKey().trim(),
				entry.getValue().trim()
			))
			.map(entry -> new java.util.AbstractMap.SimpleEntry<>(
				entry.getKey(),
				toSignalColor(entry.getValue())
			))
			.filter(entry -> entry.getValue() != null)
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	private SignalColor toSignalColor(String value) {
		String normalized = value.toUpperCase();
		if (normalized.contains("GREEN")) {
			return SignalColor.GREEN;
		}
		if (normalized.contains("YELLOW")) {
			return SignalColor.YELLOW;
		}
		if (normalized.contains("RED")) {
			return SignalColor.RED;
		}
		return null;
	}

	private String resolveMetricCode(Map<Long, String> metricIdMap, Long metricId) {
		if (metricIdMap == null || metricId == null) {
			return null;
		}
		return metricIdMap.get(metricId);
	}
}
