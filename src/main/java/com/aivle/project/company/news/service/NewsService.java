package com.aivle.project.company.news.service;

import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.news.client.NewsClient;
import com.aivle.project.company.news.dto.NewsApiResponse;
import com.aivle.project.company.news.dto.NewsAnalysisResponse;
import com.aivle.project.company.news.dto.NewsItemResponse;
import com.aivle.project.company.news.repository.NewsAnalysisRepository;
import com.aivle.project.company.news.repository.NewsArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 뉴스 분석 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsService {

    private final NewsAnalysisRepository newsAnalysisRepository;
    private final NewsArticleRepository newsArticleRepository;
    private final NewsClient newsClient;
    private final com.aivle.project.company.repository.CompaniesRepository companiesRepository;

    /**
     * AI 서버에서 뉴스 분석 데이터를 가져와 저장합니다.
     *
     * @param stockCode 기업 코드 (stock_code)
     * @return 저장된 뉴스 분석 정보
     */
    @Transactional
    public NewsAnalysisResponse fetchAndStoreNews(String stockCode) {
        // 1. stockCode로 기업 조회
        CompaniesEntity company = companiesRepository.findByStockCode(stockCode)
                .orElseThrow(() -> new IllegalArgumentException("Company not found for stockCode: " + stockCode));

        log.info("Fetching news for company: {} ({})", company.getCorpName(), stockCode);

        // 2. AI 서버 API 호출
        NewsApiResponse apiResponse = newsClient.fetchNews(stockCode, company.getCorpName());

        // 3. 분석 엔티티 생성 및 저장
        com.aivle.project.company.news.entity.NewsAnalysisEntity analysis =
                com.aivle.project.company.news.entity.NewsAnalysisEntity.builder()
                        .company(company)
                        .companyName(apiResponse.companyName())
                        .totalCount(apiResponse.totalCount())
                        .averageScore(apiResponse.averageScore() != null ?
                                BigDecimal.valueOf(apiResponse.averageScore()) : null)
                        .analyzedAt(parseToUtcLocalDateTime(apiResponse.analyzedAt(), "analyzedAt"))
                        .build();

        com.aivle.project.company.news.entity.NewsAnalysisEntity savedAnalysis =
                newsAnalysisRepository.save(analysis);

        // 4. 뉴스 기사 생성 및 저장
        List<com.aivle.project.company.news.entity.NewsArticleEntity> articles = apiResponse.news().stream()
                .map(newsItem -> createNewsArticleEntity(savedAnalysis, newsItem))
                .toList();

        newsArticleRepository.saveAll(articles);

        log.info("News saved for company: {}, analysisId: {}, articleCount: {}",
                company.getCorpName(), savedAnalysis.getId(), articles.size());

        // 5. DTO로 변환하여 반환
        return NewsAnalysisResponse.from(company.getId(), savedAnalysis, articles);
    }

    /**
     * 특정 기업의 최신 뉴스를 조회합니다.
     *
     * @param stockCode 기업 코드 (stock_code)
     * @return 최신 뉴스 분석 정보 (없을 경우 null)
     */
    @Transactional(readOnly = true)
    public NewsAnalysisResponse getLatestNews(String stockCode) {
        CompaniesEntity company = companiesRepository.findByStockCode(stockCode)
                .orElseThrow(() -> new IllegalArgumentException("Company not found for stockCode: " + stockCode));

        // analyzed_at 기준 최신 분석 조회
        com.aivle.project.company.news.entity.NewsAnalysisEntity latestAnalysis =
                newsAnalysisRepository.findTopByCompanyIdOrderByAnalyzedAtDesc(company.getId())
                        .orElse(null);

        if (latestAnalysis == null) {
            return null;
        }

        // 해당 분석의 모든 뉴스 기사 조회
        List<com.aivle.project.company.news.entity.NewsArticleEntity> articles =
                newsArticleRepository.findByNewsAnalysisIdOrderByPublishedAtDesc(latestAnalysis.getId());

        return NewsAnalysisResponse.from(company.getId(), latestAnalysis, articles);
    }

    /**
     * 특정 기간의 뉴스 분석 이력을 조회합니다.
     *
     * @param stockCode    기업 코드 (stock_code)
     * @param start        시작 일시
     * @param end          종료 일시
     * @return 뉴스 분석 이력 목록
     */
    @Transactional(readOnly = true)
    public List<NewsAnalysisResponse> getNewsHistory(String stockCode, LocalDateTime start, LocalDateTime end) {
        CompaniesEntity company = companiesRepository.findByStockCode(stockCode)
                .orElseThrow(() -> new IllegalArgumentException("Company not found for stockCode: " + stockCode));

        List<com.aivle.project.company.news.entity.NewsAnalysisEntity> analyses =
                newsAnalysisRepository.findByCompanyIdAndAnalyzedAtBetweenOrderByAnalyzedAtDesc(
                        company.getId(), start, end);

        return analyses.stream()
                .map(analysis -> {
                    List<com.aivle.project.company.news.entity.NewsArticleEntity> articles =
                            newsArticleRepository.findByNewsAnalysisIdOrderByPublishedAtDesc(analysis.getId());
                    return NewsAnalysisResponse.from(company.getId(), analysis, articles);
                })
                .collect(Collectors.toList());
    }

    /**
     * 뉴스 기사 엔티티를 생성합니다.
     */
    private com.aivle.project.company.news.entity.NewsArticleEntity createNewsArticleEntity(
            com.aivle.project.company.news.entity.NewsAnalysisEntity analysis,
            NewsItemResponse item
    ) {
        return com.aivle.project.company.news.entity.NewsArticleEntity.builder()
                .newsAnalysis(analysis)
                .title(item.title())
                .summary(item.summary())
                .score(item.score() != null ? BigDecimal.valueOf(item.score()) : null)
                .publishedAt(parseToUtcLocalDateTime(item.date(), "news.date"))
                .link(item.link())
                .sentiment(item.sentiment() != null ? item.sentiment() : "NEU")
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
