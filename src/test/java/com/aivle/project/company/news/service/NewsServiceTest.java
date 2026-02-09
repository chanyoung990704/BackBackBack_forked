package com.aivle.project.company.news.service;

import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.news.client.NewsClient;
import com.aivle.project.company.news.dto.NewsApiResponse;
import com.aivle.project.company.news.dto.NewsItemResponse;
import com.aivle.project.company.news.dto.NewsRefreshResponse;
import com.aivle.project.company.news.repository.NewsAnalysisRepository;
import com.aivle.project.company.news.repository.NewsArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NewsServiceTest {

    @Mock
    private NewsAnalysisRepository newsAnalysisRepository;

    @Mock
    private NewsArticleRepository newsArticleRepository;

    @Mock
    private NewsClient newsClient;

    @Mock
    private com.aivle.project.company.repository.CompaniesRepository companiesRepository;

    @InjectMocks
    private NewsService newsService;

    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("뉴스 분석 데이터를 성공적으로 가져와 저장한다")
    void fetchAndStoreNews_Success() {
        // given
        String stockCode = "000020";
        String companyName = "동화약품";

        CompaniesEntity company = CompaniesEntity.create(
                "000020",
                companyName,
                null,
                stockCode,
                null
        );
        setId(company, 1L);

        NewsItemResponse newsItem = new NewsItemResponse(
                "동화약품, 연구개발본부장에 장재원 전무",
                "기사의 본문이 제공되지 않아 구체적인 내용을 요약할 수 없습니다.",
                0.0007,
                "2026-01-30T09:34:00+09:00",
                "https://n.news.naver.com/mnews/article/003/0013739277?sid=102",
                "NEU"
        );

        NewsApiResponse apiResponse = new NewsApiResponse(
                companyName,
                1,
                List.of(newsItem),
                0.0007,
                "2026-02-05T14:10:57.932669"
        );

        when(companiesRepository.findByStockCode(stockCode)).thenReturn(Optional.of(company));
        when(newsClient.fetchNews(stockCode, companyName)).thenReturn(apiResponse);
        when(newsAnalysisRepository.save(any())).thenAnswer(invocation -> {
            com.aivle.project.company.news.entity.NewsAnalysisEntity saved = invocation.getArgument(0);
            setId(saved, 10L);
            setCreatedAt(saved);
            return saved;
        });
        when(newsArticleRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<com.aivle.project.company.news.entity.NewsArticleEntity> articles = invocation.getArgument(0);
            articles.forEach(NewsServiceTest::setCreatedAt);
            return articles;
        });

        // when
        var result = newsService.fetchAndStoreNews(stockCode);

        // then
        assertNotNull(result);
        assertEquals(companyName, result.companyName());
        assertEquals(1, result.totalCount());
        assertEquals(1, result.news().size());
        assertEquals("동화약품, 연구개발본부장에 장재원 전무", result.news().get(0).title());

        verify(companiesRepository, times(1)).findByStockCode(stockCode);
        verify(newsClient, times(1)).fetchNews(stockCode, companyName);
        verify(newsAnalysisRepository, times(1)).save(any());
        verify(newsArticleRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("존재하지 않는 기업 코드로 뉴스를 조회하면 예외가 발생한다")
    void fetchAndStoreNews_CompanyNotFound() {
        // given
        String stockCode = "999999";
        when(companiesRepository.findByStockCode(stockCode)).thenReturn(Optional.empty());

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            newsService.fetchAndStoreNews(stockCode);
        });

        verify(companiesRepository, times(1)).findByStockCode(stockCode);
        verify(newsClient, never()).fetchNews(anyString(), anyString());
    }

    @Test
    @DisplayName("최신 뉴스를 성공적으로 조회한다")
    void getLatestNews_Success() {
        // given
        String stockCode = "000020";
        String companyName = "동화약품";

        CompaniesEntity company = CompaniesEntity.create(
                "000020",
                companyName,
                null,
                stockCode,
                null
        );
        setId(company, 1L);

        LocalDateTime analyzedAt = LocalDateTime.now().minusDays(1);
        var analysis = com.aivle.project.company.news.entity.NewsAnalysisEntity.create(
                company,
                companyName,
                1,
                BigDecimal.valueOf(0.0007),
                analyzedAt
        );
        setId(analysis, 10L);
        setCreatedAt(analysis);

        NewsItemResponse newsItem = new NewsItemResponse(
                "동화약품, 연구개발본부장에 장재원 전무",
                "기사의 본문이 제공되지 않아 구체적인 내용을 요약할 수 없습니다.",
                0.0007,
                "2026-01-30T09:34:00+09:00",
                "https://n.news.naver.com/mnews/article/003/0013739277?sid=102",
                "NEU"
        );

        var article = com.aivle.project.company.news.entity.NewsArticleEntity.create(
                analysis,
                newsItem.title(),
                newsItem.summary(),
                BigDecimal.valueOf(newsItem.score()),
                LocalDateTime.parse("2026-01-30T00:34:00"),
                newsItem.link(),
                newsItem.sentiment()
        );
        setCreatedAt(article);

        when(companiesRepository.findByStockCode(stockCode)).thenReturn(Optional.of(company));
        when(newsAnalysisRepository.findTopByCompanyIdOrderByAnalyzedAtDesc(company.getId()))
                .thenReturn(Optional.of(analysis));
        when(newsArticleRepository.findByNewsAnalysisIdOrderByPublishedAtDesc(analysis.getId()))
                .thenReturn(List.of(article));

        // when
        var result = newsService.getLatestNews(stockCode);

        // then
        assertNotNull(result);
        assertEquals(companyName, result.companyName());
        assertEquals(1, result.totalCount());

        verify(companiesRepository, times(1)).findByStockCode(stockCode);
        verify(newsAnalysisRepository, times(1)).findTopByCompanyIdOrderByAnalyzedAtDesc(company.getId());
    }

    @Test
    @DisplayName("뉴스 분석 이력이 없는 기업의 최신 뉴스를 조회하면 null을 반환한다")
    void getLatestNews_NoHistory() {
        // given
        String stockCode = "000020";
        String companyName = "동화약품";

        CompaniesEntity company = CompaniesEntity.create(
                "000020",
                companyName,
                null,
                stockCode,
                null
        );
        setId(company, 1L);

        when(companiesRepository.findByStockCode(stockCode)).thenReturn(Optional.of(company));
        when(newsAnalysisRepository.findTopByCompanyIdOrderByAnalyzedAtDesc(company.getId()))
                .thenReturn(Optional.empty());

        // when
        var result = newsService.getLatestNews(stockCode);

        // then
        assertNull(result);

        verify(companiesRepository, times(1)).findByStockCode(stockCode);
        verify(newsAnalysisRepository, times(1)).findTopByCompanyIdOrderByAnalyzedAtDesc(company.getId());
    }

	    @Test
	    @DisplayName("특정 기간의 뉴스 분석 이력을 성공적으로 조회한다")
	    void getNewsHistory_Success() {
        // given
        String stockCode = "000020";
        String companyName = "동화약품";

        CompaniesEntity company = CompaniesEntity.create(
                "000020",
                companyName,
                null,
                stockCode,
                null
        );
        setId(company, 1L);

        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime analyzedAt = LocalDateTime.now().minusDays(1);

        var analysis = com.aivle.project.company.news.entity.NewsAnalysisEntity.create(
                company,
                companyName,
                1,
                BigDecimal.valueOf(0.0007),
                analyzedAt
        );
        setId(analysis, 10L);
        setCreatedAt(analysis);

        NewsItemResponse newsItem = new NewsItemResponse(
                "동화약품, 연구개발본부장에 장재원 전무",
                "기사의 본문이 제공되지 않아 구체적인 내용을 요약할 수 없습니다.",
                0.0007,
                "2026-01-30T09:34:00+09:00",
                "https://n.news.naver.com/mnews/article/003/0013739277?sid=102",
                "NEU"
        );

        var article = com.aivle.project.company.news.entity.NewsArticleEntity.create(
                analysis,
                newsItem.title(),
                newsItem.summary(),
                BigDecimal.valueOf(newsItem.score()),
                LocalDateTime.parse("2026-01-30T00:34:00"),
                newsItem.link(),
                newsItem.sentiment()
        );
        setCreatedAt(article);

        when(companiesRepository.findByStockCode(stockCode)).thenReturn(Optional.of(company));
        when(newsAnalysisRepository.findByCompanyIdAndAnalyzedAtBetweenOrderByAnalyzedAtDesc(
                eq(company.getId()), eq(start), eq(end)))
                .thenReturn(List.of(analysis));
        when(newsArticleRepository.findByNewsAnalysisIdOrderByPublishedAtDesc(analysis.getId()))
                .thenReturn(List.of(article));

        // when
        var result = newsService.getNewsHistory(stockCode, start, end);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(companyName, result.get(0).companyName());

        verify(companiesRepository, times(1)).findByStockCode(stockCode);
	        verify(newsAnalysisRepository, times(1))
	                .findByCompanyIdAndAnalyzedAtBetweenOrderByAnalyzedAtDesc(eq(company.getId()), eq(start), eq(end));
	    }

	@Test
	@DisplayName("재수집 시 average_score가 누락되면 기사 점수 평균으로 복구한다")
	void refreshLatestNews_RepairAverage() {
		// given
		String stockCode = "000020";
		String companyName = "동화약품";

		CompaniesEntity company = CompaniesEntity.create(
			"000020",
			companyName,
			null,
			stockCode,
			null
		);
		setId(company, 1L);

		NewsItemResponse newsItem1 = new NewsItemResponse(
			"기사1",
			"요약1",
			0.3,
			"2026-01-30T09:34:00+09:00",
			"https://n.example/1",
			"POS"
		);
		NewsItemResponse newsItem2 = new NewsItemResponse(
			"기사2",
			"요약2",
			0.1,
			"2026-01-30T10:34:00+09:00",
			"https://n.example/2",
			"NEU"
		);
		NewsApiResponse apiResponse = new NewsApiResponse(
			companyName,
			2,
			List.of(newsItem1, newsItem2),
			null,
			"2026-02-05T14:10:57.932669"
		);

		when(companiesRepository.findByStockCode(stockCode)).thenReturn(Optional.of(company));
		when(newsClient.fetchNews(stockCode, companyName)).thenReturn(apiResponse);
		when(newsAnalysisRepository.save(any())).thenAnswer(invocation -> {
			com.aivle.project.company.news.entity.NewsAnalysisEntity saved = invocation.getArgument(0);
			setId(saved, 10L);
			setCreatedAt(saved);
			return saved;
		});
			when(newsArticleRepository.saveAll(anyList())).thenAnswer(invocation -> {
				List<com.aivle.project.company.news.entity.NewsArticleEntity> articles = invocation.getArgument(0);
				articles.forEach(NewsServiceTest::setCreatedAt);
				return articles;
			});

		// NewsService 내부에서 findById 호출 시 저장된 엔티티를 반환하도록 스텁
		com.aivle.project.company.news.entity.NewsAnalysisEntity savedAnalysis =
			com.aivle.project.company.news.entity.NewsAnalysisEntity.create(
				company,
				companyName,
				2,
				null,
				LocalDateTime.now()
			);
		setId(savedAnalysis, 10L);
		setCreatedAt(savedAnalysis);
		when(newsAnalysisRepository.findById(10L)).thenReturn(Optional.of(savedAnalysis));

			var article1 = com.aivle.project.company.news.entity.NewsArticleEntity.create(
				savedAnalysis,
				"기사1",
				"요약1",
			BigDecimal.valueOf(0.3),
			LocalDateTime.now(),
				"https://n.example/1",
				"POS"
			);
			setCreatedAt(article1);
			var article2 = com.aivle.project.company.news.entity.NewsArticleEntity.create(
				savedAnalysis,
				"기사2",
			"요약2",
			BigDecimal.valueOf(0.1),
			LocalDateTime.now(),
				"https://n.example/2",
				"NEU"
			);
			setCreatedAt(article2);
		when(newsArticleRepository.findByNewsAnalysisIdOrderByPublishedAtDesc(10L))
			.thenReturn(List.of(article1, article2));
		when(newsAnalysisRepository.findTopByCompanyIdOrderByAnalyzedAtDesc(1L))
			.thenReturn(Optional.of(savedAnalysis));

		// when
		NewsRefreshResponse result = newsService.refreshLatestNews(stockCode);

		// then
		assertNotNull(result);
		assertTrue(result.averageScoreRepaired());
		assertEquals(0.2d, result.analysis().averageScore(), 0.000001d);
	}

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (closeable != null) {
            closeable.close();
        }
    }

    private static void setId(Object target, Long id) {
        ReflectionTestUtils.setField(target, "id", id);
    }

    private static void setCreatedAt(Object target) {
        ReflectionTestUtils.setField(target, "createdAt", LocalDateTime.now(ZoneOffset.UTC));
    }
}
