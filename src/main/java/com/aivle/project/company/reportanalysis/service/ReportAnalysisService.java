package com.aivle.project.company.reportanalysis.service;

import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.news.client.NewsClient;
import com.aivle.project.company.news.dto.NewsItemResponse;
import com.aivle.project.company.reportanalysis.dto.ReportApiResponse;
import com.aivle.project.company.reportanalysis.dto.ReportAnalysisResponse;
import com.aivle.project.company.reportanalysis.entity.ReportAnalysisEntity;
import com.aivle.project.company.reportanalysis.entity.ReportContentEntity;
import com.aivle.project.company.reportanalysis.repository.ReportAnalysisRepository;
import com.aivle.project.company.reportanalysis.repository.ReportContentRepository;
import com.aivle.project.company.repository.CompaniesRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사업보고서 분석 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportAnalysisService {

	private final ReportAnalysisRepository reportAnalysisRepository;
	private final ReportContentRepository reportContentRepository;
	private final NewsClient newsClient;
	private final CompaniesRepository companiesRepository;

	/**
	 * AI 서버에서 사업보고서 분석 데이터를 가져와 저장합니다.
	 *
	 * @param stockCode 기업 코드 (stock_code)
	 * @return 저장된 사업보고서 분석 정보
	 */
	@Transactional
	public ReportAnalysisResponse fetchAndStoreReport(String stockCode) {
		CompaniesEntity company = companiesRepository.findByStockCode(stockCode)
			.orElseThrow(() -> new IllegalArgumentException("Company not found for stockCode: " + stockCode));

		log.info("Fetching report analysis for company: {} ({})", company.getCorpName(), stockCode);

		ReportApiResponse apiResponse = fetchReportWithRetry(stockCode);

		ReportAnalysisEntity analysis = ReportAnalysisEntity.builder()
			.company(company)
			.companyName(apiResponse.companyName())
			.totalCount(apiResponse.totalCount())
			.averageScore(apiResponse.averageScore() != null
				? BigDecimal.valueOf(apiResponse.averageScore())
				: null)
			.analyzedAt(parseToUtcLocalDateTime(apiResponse.analyzedAt(), "analyzedAt"))
			.build();

		ReportAnalysisEntity savedAnalysis = reportAnalysisRepository.save(analysis);

		List<ReportContentEntity> contents = buildReportContents(savedAnalysis, apiResponse);

		reportContentRepository.saveAll(contents);

		log.info("Report analysis saved for company: {}, analysisId: {}, contentCount: {}",
			company.getCorpName(), savedAnalysis.getId(), contents.size());

		return ReportAnalysisResponse.from(company.getId(), savedAnalysis, contents);
	}

	private ReportApiResponse fetchReportWithRetry(String stockCode) {
		int maxAttempts = 3;
		long delayMillis = 500L;
		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			ReportApiResponse response = newsClient.fetchReport(stockCode);
			if (response != null && response.news() != null && !response.news().isEmpty()) {
				return response;
			}
			if (attempt < maxAttempts) {
				sleep(delayMillis);
			}
		}
		return newsClient.fetchReport(stockCode);
	}

	private void sleep(long delayMillis) {
		try {
			Thread.sleep(delayMillis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Retry sleep interrupted", e);
		}
	}

	/**
	 * 특정 기업의 최신 사업보고서 분석을 조회합니다.
	 *
	 * @param stockCode 기업 코드 (stock_code)
	 * @return 최신 사업보고서 분석 정보 (없을 경우 null)
	 */
	@Transactional(readOnly = true)
	public ReportAnalysisResponse getLatestReport(String stockCode) {
		CompaniesEntity company = companiesRepository.findByStockCode(stockCode)
			.orElseThrow(() -> new IllegalArgumentException("Company not found for stockCode: " + stockCode));

		ReportAnalysisEntity latestAnalysis = reportAnalysisRepository
			.findTopByCompanyIdOrderByAnalyzedAtDesc(company.getId())
			.orElse(null);

		if (latestAnalysis == null) {
			return null;
		}

		List<ReportContentEntity> contents = reportContentRepository
			.findByReportAnalysisIdOrderByPublishedAtDesc(latestAnalysis.getId());

		return ReportAnalysisResponse.from(company.getId(), latestAnalysis, contents);
	}

	private ReportContentEntity createReportContentEntity(
		ReportAnalysisEntity analysis,
		NewsItemResponse item
	) {
		return ReportContentEntity.builder()
			.reportAnalysis(analysis)
			.title(item.title())
			.summary(item.summary())
			.score(item.score() != null ? BigDecimal.valueOf(item.score()) : null)
			.publishedAt(parseToUtcLocalDateTime(item.date(), "news.date"))
			.link(item.link())
			.sentiment(item.sentiment())
			.build();
	}

	private List<ReportContentEntity> buildReportContents(
		ReportAnalysisEntity savedAnalysis,
		ReportApiResponse apiResponse
	) {
		if (apiResponse == null || apiResponse.news() == null || apiResponse.news().isEmpty()) {
			log.warn("Report analysis response has no contents for analysisId: {}", savedAnalysis.getId());
			return List.of(createPlaceholderContent(savedAnalysis));
		}
		return apiResponse.news().stream()
			.map(item -> createReportContentEntity(savedAnalysis, item))
			.toList();
	}

	private ReportContentEntity createPlaceholderContent(ReportAnalysisEntity analysis) {
		return ReportContentEntity.builder()
			.reportAnalysis(analysis)
			.title("요약 데이터 없음")
			.summary("요약 데이터가 제공되지 않았습니다.")
			.score(null)
			.publishedAt(analysis.getAnalyzedAt())
			.link(null)
			.sentiment(null)
			.build();
	}

	// AI 서버가 offset 포함/미포함 datetime을 혼합 응답하므로 둘 다 허용합니다.
	private LocalDateTime parseToUtcLocalDateTime(String rawDateTime, String fieldName) {
		if (rawDateTime == null || rawDateTime.isBlank()) {
			return null;
		}
		try {
			return OffsetDateTime.parse(rawDateTime).atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
		} catch (DateTimeParseException ignored) {
			try {
				return LocalDateTime.parse(rawDateTime);
			} catch (DateTimeParseException e) {
				throw new IllegalArgumentException("Invalid datetime format for " + fieldName + ": " + rawDateTime, e);
			}
		}
	}
}
