package com.aivle.project.company.news.repository;

import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.news.entity.NewsAnalysisEntity;
import com.aivle.project.company.news.entity.NewsArticleEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest
class NewsArticleRepositoryTest {

    @Autowired
    private NewsAnalysisRepository newsAnalysisRepository;

    @Autowired
    private NewsArticleRepository newsArticleRepository;

    @Autowired
    private com.aivle.project.company.repository.CompaniesRepository companiesRepository;

    @BeforeEach
    void setUp() {
        newsArticleRepository.deleteAllInBatch();
        newsAnalysisRepository.deleteAllInBatch();
        companiesRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("뉴스 기사를 성공적으로 저장하고 조회한다")
    void saveAndFindByNewsAnalysisId() {
        // given
        CompaniesEntity company = createCompany("동화약품", "000020");
        NewsAnalysisEntity analysis = NewsAnalysisEntity.builder()
                .company(company)
                .companyName(company.getCorpName())
                .totalCount(2)
                .analyzedAt(LocalDateTime.now())
                .build();
        NewsAnalysisEntity savedAnalysis = newsAnalysisRepository.save(analysis);

        NewsArticleEntity article1 = NewsArticleEntity.builder()
                .newsAnalysis(savedAnalysis)
                .title("기사 제목 1")
                .summary("요약 1")
                .score(BigDecimal.valueOf(0.1))
                .publishedAt(LocalDateTime.now())
                .link("https://example.com/1")
                .sentiment("POS")
                .build();

        NewsArticleEntity article2 = NewsArticleEntity.builder()
                .newsAnalysis(savedAnalysis)
                .title("기사 제목 2")
                .summary("요약 2")
                .score(BigDecimal.valueOf(-0.1))
                .publishedAt(LocalDateTime.now().minusDays(1))
                .link("https://example.com/2")
                .sentiment("NEG")
                .build();

        newsArticleRepository.saveAll(List.of(article1, article2));

        // when
        List<NewsArticleEntity> found = newsArticleRepository
                .findByNewsAnalysisIdOrderByPublishedAtDesc(savedAnalysis.getId());

        // then
        assertEquals(2, found.size());
        assertEquals("기사 제목 1", found.get(0).getTitle());
        assertEquals("기사 제목 2", found.get(1).getTitle());
    }

    @Test
    @DisplayName("특정 분석의 뉴스를 감성별로 조회한다")
    void findByNewsAnalysisIdAndSentimentOrderByPublishedAtDesc() {
        // given
        CompaniesEntity company = createCompany("동화약품", "000020");
        NewsAnalysisEntity analysis = NewsAnalysisEntity.builder()
                .company(company)
                .companyName(company.getCorpName())
                .totalCount(3)
                .analyzedAt(LocalDateTime.now())
                .build();
        NewsAnalysisEntity savedAnalysis = newsAnalysisRepository.save(analysis);

        NewsArticleEntity posArticle = NewsArticleEntity.builder()
                .newsAnalysis(savedAnalysis)
                .title("긍정 기사")
                .summary("긍정 요약")
                .score(BigDecimal.valueOf(0.5))
                .publishedAt(LocalDateTime.now())
                .link("https://example.com/pos")
                .sentiment("POS")
                .build();

        NewsArticleEntity neuArticle = NewsArticleEntity.builder()
                .newsAnalysis(savedAnalysis)
                .title("중립 기사")
                .summary("중립 요약")
                .score(BigDecimal.valueOf(0))
                .publishedAt(LocalDateTime.now())
                .link("https://example.com/neu")
                .sentiment("NEU")
                .build();

        newsArticleRepository.saveAll(List.of(posArticle, neuArticle));

        // when
        List<NewsArticleEntity> posArticles = newsArticleRepository
                .findByNewsAnalysisIdAndSentimentOrderByPublishedAtDesc(savedAnalysis.getId(), "POS");

        // then
        assertEquals(1, posArticles.size());
        assertEquals("POS", posArticles.get(0).getSentiment());
    }

    @Test
    @DisplayName("뉴스 분석을 삭제하면 관련 뉴스 기사도 함께 삭제된다")
    void deleteNewsAnalysisCascadesToNewsArticles() {
        // given
        CompaniesEntity company = createCompany("동화약품", "000020");
        NewsAnalysisEntity analysis = NewsAnalysisEntity.builder()
                .company(company)
                .companyName(company.getCorpName())
                .totalCount(1)
                .analyzedAt(LocalDateTime.now())
                .build();
        NewsAnalysisEntity savedAnalysis = newsAnalysisRepository.save(analysis);

        NewsArticleEntity article = NewsArticleEntity.builder()
                .newsAnalysis(savedAnalysis)
                .title("기사 제목")
                .summary("요약")
                .score(BigDecimal.valueOf(0.1))
                .publishedAt(LocalDateTime.now())
                .link("https://example.com/1")
                .sentiment("NEU")
                .build();
        newsArticleRepository.save(article);

        // when
        newsAnalysisRepository.delete(savedAnalysis);

        // then
        assertEquals(0, newsArticleRepository.count());
    }

    private CompaniesEntity createCompany(String name, String stockCode) {
        CompaniesEntity company = CompaniesEntity.create(
                stockCode,
                name,
                null,
                stockCode,
                null
        );
        return companiesRepository.save(company);
    }
}
