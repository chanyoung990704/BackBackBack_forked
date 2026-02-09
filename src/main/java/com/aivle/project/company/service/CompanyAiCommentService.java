package com.aivle.project.company.service;

import com.aivle.project.company.client.AiServerClient;
import com.aivle.project.company.dto.AiCommentResponse;
import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.keymetric.entity.CompanyKeyMetricEntity;
import com.aivle.project.company.repository.CompaniesRepository;
import com.aivle.project.quarter.support.QuarterCalculator;
import com.aivle.project.quarter.support.YearQuarter;
import com.aivle.project.report.repository.CompanyReportMetricValuesRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 종합 코멘트 캐시 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyAiCommentService {

	private final CompaniesRepository companiesRepository;
	private final CompanyReportMetricValuesRepository companyReportMetricValuesRepository;
	private final CompanyHealthScoreCacheService companyHealthScoreCacheService;
	private final AiServerClient aiServerClient;

	/**
	 * 요청 분기의 AI 코멘트를 보장한다.
	 */
	@Transactional
	public String ensureAiCommentCached(Long companyId, String period) {
		CompaniesEntity company = companiesRepository.findById(companyId)
			.orElseThrow(() -> new IllegalArgumentException("Company not found: " + companyId));

		int targetQuarterKey = resolveTargetQuarterKey(company.getStockCode(), period);
		CompanyKeyMetricEntity keyMetric = companyHealthScoreCacheService.getOrCreateKeyMetric(companyId, targetQuarterKey);
		if (keyMetric.getAiComment() != null && !keyMetric.getAiComment().isBlank()) {
			return keyMetric.getAiComment();
		}

		AiCommentResponse response = aiServerClient.getAiComment(company.getStockCode(), String.valueOf(targetQuarterKey));
		if (response == null || response.aiComment() == null || response.aiComment().isBlank()) {
			log.warn("Empty AI comment response for company: {}, quarterKey: {}", company.getStockCode(), targetQuarterKey);
			return null;
		}

		keyMetric.applyAiAnalysis(
			response.aiComment(),
			null,
			null,
			null,
			LocalDateTime.now()
		);
		return keyMetric.getAiComment();
	}

	private int resolveTargetQuarterKey(String stockCode, String period) {
		if (period != null && !period.isBlank()) {
			return parseQuarterKey(period);
		}
		return companyReportMetricValuesRepository.findMaxActualQuarterKeyByStockCode(stockCode)
			.orElseThrow(() -> new IllegalArgumentException("Actual quarter not found for stockCode: " + stockCode));
	}

	private int parseQuarterKey(String period) {
		String normalized = normalizeQuarterKey(period);
		int parsed = Integer.parseInt(normalized);
		YearQuarter yearQuarter = QuarterCalculator.parseQuarterKey(parsed);
		return yearQuarter.toQuarterKey();
	}

	private String normalizeQuarterKey(String period) {
		String trimmed = period.trim();
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
}
