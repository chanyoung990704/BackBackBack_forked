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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest
class NewsAnalysisRepositoryTest {

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
    @DisplayName("뉴스 분석을 성공적으로 저장하고 조회한다")
    void saveAndFindById() {
        // given
        CompaniesEntity company = createCompany("동화약품", "000020");
        NewsAnalysisEntity analysis = NewsAnalysisEntity.builder()
                .company(company)
                .companyName(company.getCorpName())
                .totalCount(3)
                .averageScore(BigDecimal.valueOf(0.0041))
                .analyzedAt(LocalDateTime.now())
                .build();

        NewsAnalysisEntity saved = newsAnalysisRepository.save(analysis);

        // when
        Optional<NewsAnalysisEntity> found = newsAnalysisRepository.findById(saved.getId());

        // then
        assertTrue(found.isPresent());
        assertEquals(company.getId(), found.get().getCompany().getId());
        assertEquals(3, found.get().getTotalCount());
    }

    @Test
    @DisplayName("특정 기업의 최신 분석을 조회한다")
    void findTopByCompanyIdOrderByAnalyzedAtDesc() {
        // given
        CompaniesEntity company = createCompany("동화약품", "000020");

        NewsAnalysisEntity oldAnalysis = NewsAnalysisEntity.builder()
                .company(company)
                .companyName(company.getCorpName())
                .totalCount(2)
                .analyzedAt(LocalDateTime.now().minusDays(2))
                .build();
        newsAnalysisRepository.save(oldAnalysis);

        NewsAnalysisEntity newAnalysis = NewsAnalysisEntity.builder()
                .company(company)
                .companyName(company.getCorpName())
                .totalCount(3)
                .analyzedAt(LocalDateTime.now().minusDays(1))
                .build();
        newsAnalysisRepository.save(newAnalysis);

        // when
        Optional<NewsAnalysisEntity> latest = newsAnalysisRepository
                .findTopByCompanyIdOrderByAnalyzedAtDesc(company.getId());

        // then
        assertTrue(latest.isPresent());
        assertEquals(3, latest.get().getTotalCount());
    }

    @Test
    @DisplayName("특정 기간의 분석 이력을 조회한다")
    void findByCompanyIdAndAnalyzedAtBetweenOrderByAnalyzedAtDesc() {
        // given
        CompaniesEntity company = createCompany("동화약품", "000020");

        LocalDateTime now = LocalDateTime.of(2026, 2, 1, 0, 0, 0);
        NewsAnalysisEntity analysis1 = NewsAnalysisEntity.builder()
                .company(company)
                .companyName(company.getCorpName())
                .totalCount(1)
                .analyzedAt(now.minusDays(3))
                .build();
        newsAnalysisRepository.save(analysis1);

        NewsAnalysisEntity analysis2 = NewsAnalysisEntity.builder()
                .company(company)
                .companyName(company.getCorpName())
                .totalCount(2)
                .analyzedAt(now.minusDays(2))
                .build();
        newsAnalysisRepository.save(analysis2);

        NewsAnalysisEntity analysis3 = NewsAnalysisEntity.builder()
                .company(company)
                .companyName(company.getCorpName())
                .totalCount(3)
                .analyzedAt(now.minusDays(1))
                .build();
        newsAnalysisRepository.save(analysis3);

        LocalDateTime start = now.minusDays(3);
        LocalDateTime end = now.minusDays(1);

        // when
        List<NewsAnalysisEntity> analyses = newsAnalysisRepository
                .findByCompanyIdAndAnalyzedAtBetweenOrderByAnalyzedAtDesc(company.getId(), start, end);

        // then
        assertEquals(3, analyses.size());
        assertEquals(3, analyses.get(0).getTotalCount());
    }

    @Test
    @DisplayName("기업을 삭제하면 관련 뉴스 분석도 함께 삭제된다")
    void deleteCompanyCascadesToNewsAnalyses() {
        // given
        CompaniesEntity company = createCompany("동화약품", "000020");
        NewsAnalysisEntity analysis = NewsAnalysisEntity.builder()
                .company(company)
                .companyName(company.getCorpName())
                .totalCount(1)
                .analyzedAt(LocalDateTime.now())
                .build();
        NewsAnalysisEntity saved = newsAnalysisRepository.save(analysis);

        // when
        companiesRepository.delete(company);

        // then
        assertFalse(newsAnalysisRepository.findById(saved.getId()).isPresent());
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
