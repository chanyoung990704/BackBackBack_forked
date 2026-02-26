package com.aivle.project.company.insight.service;

import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.insight.dto.CompanyInsightDto;
import com.aivle.project.company.insight.dto.CompanyInsightType;
import com.aivle.project.common.error.ExternalAiUnavailableException;
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
import com.aivle.project.company.service.CompanyReputationScoreService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
	private final CompanyReputationScoreService companyReputationScoreService;

	/**
	 * 쓰기/조회 통합 경로 (하위 호환용).
	 */
	@Transactional
	public InsightResult getInsights(
		Long companyId,
		int newsPage,
		int newsSize,
		int reportPage,
		int reportSize,
		boolean refresh
	) {
		CompanyIdentity companyIdentity = resolveCompanyIdentity(companyId);
		ExternalAiUnavailableException externalFailure = ensureInsightData(companyIdentity, refresh);
		return loadInsights(companyId, newsPage, newsSize, reportPage, reportSize, externalFailure);
	}

	/**
	 * 인사이트 생성/갱신 단계 (쓰기 트랜잭션).
	 */
	@Transactional
	public ExternalAiUnavailableException ensureInsightData(Long companyId, boolean refresh) {
		CompanyIdentity companyIdentity = resolveCompanyIdentity(companyId);
		return ensureInsightData(companyIdentity, refresh);
	}

	/**
	 * 인사이트 조회/조립 단계 (읽기 전용 트랜잭션).
	 */
	@Transactional(readOnly = true)
	public InsightResult loadInsights(
		Long companyId,
		int newsPage,
		int newsSize,
		int reportPage,
		int reportSize,
		ExternalAiUnavailableException externalFailure
	) {
		Optional<ReportAnalysisEntity> latestReportOpt = reportAnalysisRepository
			.findTopByCompanyIdOrderByAnalyzedAtDesc(companyId);
		Optional<NewsAnalysisEntity> latestNewsOpt = newsAnalysisRepository
			.findTopByCompanyIdOrderByAnalyzedAtDesc(companyId);
		ReportAnalysisEntity latestReport = latestReportOpt.orElse(null);
		NewsAnalysisEntity latestNews = latestNewsOpt.orElse(null);

		if (latestReport == null) {
			latestReport = reportContentRepository
				.findTopByReportAnalysisCompanyIdOrderByPublishedAtDesc(companyId)
				.map(ReportContentEntity::getReportAnalysis)
				.orElse(null);
		}
		if (latestNews == null) {
			latestNews = newsArticleRepository
				.findTopByNewsAnalysisCompanyIdOrderByPublishedAtDesc(companyId)
				.map(NewsArticleEntity::getNewsAnalysis)
				.orElse(null);
		}

		List<CompanyInsightDto> items = new java.util.ArrayList<>();

		if (latestReport != null) {
			Page<ReportContentEntity> reportContents = reportContentRepository
				.findByReportAnalysisIdOrderByPublishedAtDesc(latestReport.getId(), PageRequest.of(reportPage, reportSize));
			for (ReportContentEntity content : reportContents.getContent()) {
				items.add(CompanyInsightDto.builder()
					.id(content.getId())
					.type(CompanyInsightType.REPORT)
					.title(content.getTitle())
					.body(content.getSummary())
					.content(null)
					.source(null)
					.publishedAt(resolvePublishedAt(content.getPublishedAt(), latestReport.getAnalyzedAt()))
					.url(content.getLink())
					.build());
			}
		}

		if (latestNews != null) {
			Page<NewsArticleEntity> newsArticles = newsArticleRepository
				.findByNewsAnalysisIdOrderByPublishedAtDesc(latestNews.getId(), PageRequest.of(newsPage, newsSize));
			for (NewsArticleEntity article : newsArticles.getContent()) {
				items.add(CompanyInsightDto.builder()
					.id(article.getId())
					.type(CompanyInsightType.NEWS)
					.title(article.getTitle())
					.body(null)
					.content(article.getSummary())
					.source(article.getSentiment())
					.publishedAt(resolvePublishedAt(article.getPublishedAt(), latestNews.getAnalyzedAt()))
					.url(article.getLink())
					.build());
			}
		}

		if (items.isEmpty() && externalFailure != null) {
			throw externalFailure;
		}
		return new InsightResult(items, latestNews != null ? latestNews.getAverageScore() : null, false);
	}

	private CompanyIdentity resolveCompanyIdentity(Long companyId) {
		CompaniesEntity company = companiesRepository.findById(companyId)
			.orElseThrow(() -> new IllegalArgumentException("Company not found for id: " + companyId));
		String stockCode = company.getStockCode();
		if (stockCode == null || stockCode.isBlank()) {
			throw new IllegalArgumentException("Company has no stockCode: " + companyId);
		}
		return new CompanyIdentity(companyId, stockCode);
	}

	private ExternalAiUnavailableException ensureInsightData(CompanyIdentity companyIdentity, boolean refresh) {
		Long companyId = companyIdentity.companyId();
		String stockCode = companyIdentity.stockCode();
		ExternalAiUnavailableException externalFailure = null;

		if (refresh) {
			try {
				newsService.refreshLatestNews(stockCode);
			} catch (ExternalAiUnavailableException ex) {
				externalFailure = ex;
				log.warn("인사이트 강제 갱신 중 뉴스 수집 실패: companyId={}, stockCode={}, reasonCode={}",
					companyId, stockCode, ex.getReasonCode());
			}
			try {
				reportAnalysisService.fetchAndStoreReport(stockCode);
			} catch (ExternalAiUnavailableException ex) {
				if (externalFailure == null) {
					externalFailure = ex;
				}
				log.warn("인사이트 강제 갱신 중 보고서 수집 실패: companyId={}, stockCode={}, reasonCode={}",
					companyId, stockCode, ex.getReasonCode());
			}
		}

		Optional<ReportAnalysisEntity> latestReportOpt = reportAnalysisRepository
			.findTopByCompanyIdOrderByAnalyzedAtDesc(companyId);
		Optional<NewsAnalysisEntity> latestNewsOpt = newsAnalysisRepository
			.findTopByCompanyIdOrderByAnalyzedAtDesc(companyId);

		boolean hasReport = latestReportOpt.isPresent()
			&& reportContentRepository.existsByReportAnalysisId(latestReportOpt.get().getId());
		boolean hasNews = latestNewsOpt.isPresent()
			&& newsArticleRepository.existsByNewsAnalysisId(latestNewsOpt.get().getId());

		if (!hasReport) {
			hasReport = reportContentRepository.findTopByReportAnalysisCompanyIdOrderByPublishedAtDesc(companyId).isPresent();
		}
		if (!hasNews) {
			hasNews = newsArticleRepository.findTopByNewsAnalysisCompanyIdOrderByPublishedAtDesc(companyId).isPresent();
		}

		if (!hasReport) {
			try {
				reportAnalysisService.fetchAndStoreReport(stockCode);
			} catch (ExternalAiUnavailableException ex) {
				if (externalFailure == null) {
					externalFailure = ex;
				}
				log.warn("인사이트 조회 중 보고서 수집 실패: companyId={}, stockCode={}, reasonCode={}",
					companyId, stockCode, ex.getReasonCode());
			}
		}
		if (!hasNews) {
			try {
				newsService.refreshLatestNews(stockCode);
			} catch (ExternalAiUnavailableException ex) {
				if (externalFailure == null) {
					externalFailure = ex;
				}
				log.warn("인사이트 조회 중 뉴스 수집 실패: companyId={}, stockCode={}, reasonCode={}",
					companyId, stockCode, ex.getReasonCode());
			}
		}
		try {
			companyReputationScoreService.syncExternalHealthScoreIfPresent(companyId, stockCode);
		} catch (ExternalAiUnavailableException ex) {
			if (externalFailure == null) {
				externalFailure = ex;
			}
			log.warn("인사이트 조회 중 평판 점수 동기화 실패: companyId={}, stockCode={}, reasonCode={}",
				companyId, stockCode, ex.getReasonCode());
		}
		return externalFailure;
	}

	private LocalDateTime resolvePublishedAt(LocalDateTime publishedAt, LocalDateTime fallback) {
		return publishedAt != null ? publishedAt : fallback;
	}

	private record CompanyIdentity(Long companyId, String stockCode) {
	}

	public record InsightResult(List<CompanyInsightDto> items, java.math.BigDecimal averageScore, boolean processing) {
	}
}
