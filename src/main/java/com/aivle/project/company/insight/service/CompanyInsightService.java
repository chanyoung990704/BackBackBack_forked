package com.aivle.project.company.insight.service;

import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.insight.dto.CompanyInsightItem;
import com.aivle.project.company.insight.dto.CompanyInsightType;
import com.aivle.project.company.news.entity.NewsAnalysisEntity;
import com.aivle.project.company.news.entity.NewsArticleEntity;
import com.aivle.project.company.news.repository.NewsAnalysisRepository;
import com.aivle.project.company.news.repository.NewsArticleRepository;
import com.aivle.project.company.news.service.NewsService;
import com.aivle.project.company.reportanalysis.entity.ReportAnalysisEntity;
import com.aivle.project.company.reportanalysis.entity.ReportContentEntity;
import com.aivle.project.company.reportanalysis.repository.ReportAnalysisRepository;
import com.aivle.project.company.reportanalysis.repository.ReportContentRepository;
import com.aivle.project.company.reportanalysis.service.ReportAnalysisService;
import com.aivle.project.company.repository.CompaniesRepository;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * 기업 인사이트 조회 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyInsightService {

	private final CompaniesRepository companiesRepository;
	private final NewsAnalysisRepository newsAnalysisRepository;
	private final NewsArticleRepository newsArticleRepository;
	private final ReportAnalysisRepository reportAnalysisRepository;
	private final ReportContentRepository reportContentRepository;
	private final NewsService newsService;
	private final ReportAnalysisService reportAnalysisService;

	@Qualifier("insightExecutor")
	private final Executor insightExecutor;

	public InsightResult getInsights(
		Long companyId,
		int newsPage,
		int newsSize,
		int reportPage,
		int reportSize
	) {
		CompaniesEntity company = companiesRepository.findById(companyId)
			.orElseThrow(() -> new IllegalArgumentException("Company not found for id: " + companyId));

		String stockCode = company.getStockCode();
		if (stockCode == null || stockCode.isBlank()) {
			throw new IllegalArgumentException("Company has no stockCode: " + companyId);
		}

		Optional<ReportAnalysisEntity> latestReportOpt = reportAnalysisRepository
			.findTopByCompanyIdOrderByAnalyzedAtDesc(companyId);
		Optional<NewsAnalysisEntity> latestNewsOpt = newsAnalysisRepository
			.findTopByCompanyIdOrderByAnalyzedAtDesc(companyId);

		boolean hasReport = latestReportOpt.isPresent()
			&& reportContentRepository.existsByReportAnalysisId(latestReportOpt.get().getId());
		boolean hasNews = latestNewsOpt.isPresent()
			&& newsArticleRepository.existsByNewsAnalysisId(latestNewsOpt.get().getId());

		boolean shouldFetch = !hasReport || !hasNews;
		List<CompletableFuture<?>> futures = new ArrayList<>();
		if (!hasReport) {
			futures.add(CompletableFuture.runAsync(() -> reportAnalysisService.fetchAndStoreReport(stockCode), insightExecutor));
		}
		if (!hasNews) {
			futures.add(CompletableFuture.runAsync(() -> newsService.fetchAndStoreNews(stockCode), insightExecutor));
		}
		if (!futures.isEmpty()) {
			CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
		}

		ReportAnalysisEntity latestReport = null;
		NewsAnalysisEntity latestNews = null;
		int maxPollAttempts = 10;
		long pollDelayMillis = 1000L;
		for (int attempt = 1; attempt <= maxPollAttempts; attempt++) {
			latestReport = reportAnalysisRepository
				.findTopByCompanyIdOrderByAnalyzedAtDesc(companyId)
				.orElse(null);
			latestNews = newsAnalysisRepository
				.findTopByCompanyIdOrderByAnalyzedAtDesc(companyId)
				.orElse(null);

			boolean reportReady = latestReport != null
				&& reportContentRepository.existsByReportAnalysisId(latestReport.getId());
			boolean newsReady = latestNews != null
				&& newsArticleRepository.existsByNewsAnalysisId(latestNews.getId());

			if (reportReady || newsReady) {
				break;
			}
			if (attempt < maxPollAttempts) {
				sleep(pollDelayMillis);
			}
		}

		List<CompanyInsightItem> items = new ArrayList<>();

		if (latestReport != null) {
			Page<ReportContentEntity> reportContents = reportContentRepository
				.findByReportAnalysisIdOrderByPublishedAtDesc(latestReport.getId(), PageRequest.of(reportPage, reportSize));
			for (ReportContentEntity content : reportContents.getContent()) {
				items.add(new CompanyInsightItem(
					content.getId(),
					CompanyInsightType.REPORT,
					content.getTitle(),
					null,
					content.getSummary(),
					null,
					(resolvePublishedAt(content.getPublishedAt(), latestReport.getAnalyzedAt())),
					content.getLink()
				));
			}
		}

		if (latestNews != null) {
			Page<NewsArticleEntity> newsArticles = newsArticleRepository
				.findByNewsAnalysisIdOrderByPublishedAtDesc(latestNews.getId(), PageRequest.of(newsPage, newsSize));
			for (NewsArticleEntity article : newsArticles.getContent()) {
				items.add(new CompanyInsightItem(
					article.getId(),
					CompanyInsightType.NEWS,
					article.getTitle(),
					article.getSummary(),
					null,
					article.getSentiment(),
					(resolvePublishedAt(article.getPublishedAt(), latestNews.getAnalyzedAt())),
					article.getLink()
				));
			}
		}

		boolean processing = shouldFetch && items.isEmpty();
		return new InsightResult(items, processing);
	}

	private String resolvePublishedAt(java.time.LocalDateTime publishedAt, java.time.LocalDateTime fallback) {
		java.time.LocalDateTime target = publishedAt != null ? publishedAt : fallback;
		return target != null ? target.atZone(ZoneOffset.UTC).toOffsetDateTime().toString() : null;
	}

	private void sleep(long delayMillis) {
		try {
			Thread.sleep(delayMillis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Insight polling interrupted", e);
		}
	}

	public record InsightResult(List<CompanyInsightItem> items, boolean processing) {
	}
}
