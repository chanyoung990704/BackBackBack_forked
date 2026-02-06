package com.aivle.project.company.news.controller;

import com.aivle.project.common.dto.ApiResponse;
import com.aivle.project.company.news.dto.NewsAnalysisResponse;
import com.aivle.project.company.news.service.NewsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NewsControllerTest {

    @Mock
    private NewsService newsService;

    @InjectMocks
    private NewsController newsController;

    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("뉴스 분석 데이터를 성공적으로 수집한다")
    void fetchNews_Success() {
        // given
        String stockCode = "000020";
        String companyName = "동화약품";

        NewsAnalysisResponse response = new NewsAnalysisResponse(
                1L,
                100L,
                companyName,
                3,
                0.0041,
                OffsetDateTime.now(),
                List.of(),
                OffsetDateTime.now()
        );

        when(newsService.fetchAndStoreNews(stockCode)).thenReturn(response);

        // when
        ResponseEntity<ApiResponse<NewsAnalysisResponse>> result = newsController.fetchNews(stockCode);

        // then
        assertEquals(200, result.getStatusCode().value());
        assertTrue(result.getBody().success());
        assertNotNull(result.getBody().data());
        assertEquals(companyName, result.getBody().data().companyName());

        verify(newsService, times(1)).fetchAndStoreNews(stockCode);
    }

    @Test
    @DisplayName("최신 뉴스를 성공적으로 조회한다")
    void getLatestNews_Success() {
        // given
        String stockCode = "000020";
        String companyName = "동화약품";

        NewsAnalysisResponse response = new NewsAnalysisResponse(
                1L,
                100L,
                companyName,
                3,
                0.0041,
                OffsetDateTime.now(),
                List.of(),
                OffsetDateTime.now()
        );

        when(newsService.getLatestNews(stockCode)).thenReturn(response);

        // when
        ResponseEntity<ApiResponse<NewsAnalysisResponse>> result = newsController.getLatestNews(stockCode);

        // then
        assertEquals(200, result.getStatusCode().value());
        assertTrue(result.getBody().success());
        assertNotNull(result.getBody().data());
        assertEquals(companyName, result.getBody().data().companyName());

        verify(newsService, times(1)).getLatestNews(stockCode);
    }

    @Test
    @DisplayName("뉴스 분석 이력이 없는 경우 에러 응답을 반환한다")
    void getLatestNews_NoData() {
        // given
        String stockCode = "000020";
        when(newsService.getLatestNews(stockCode)).thenReturn(null);

        // when
        ResponseEntity<ApiResponse<NewsAnalysisResponse>> result = newsController.getLatestNews(stockCode);

        // then
        assertEquals(200, result.getStatusCode().value());
        assertFalse(result.getBody().success());
        assertNull(result.getBody().data());
        assertNotNull(result.getBody().error());

        verify(newsService, times(1)).getLatestNews(stockCode);
    }

    @Test
    @DisplayName("특정 기간의 뉴스 분석 이력을 성공적으로 조회한다")
    void getNewsHistory_Success() {
        // given
        String stockCode = "000020";
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now();

        NewsAnalysisResponse response = new NewsAnalysisResponse(
                1L,
                100L,
                "동화약품",
                3,
                0.0041,
                OffsetDateTime.now(),
                List.of(),
                OffsetDateTime.now()
        );

        when(newsService.getNewsHistory(stockCode, start, end)).thenReturn(List.of(response));

        // when
        ResponseEntity<ApiResponse<List<NewsAnalysisResponse>>> result = newsController.getNewsHistory(stockCode, start, end);

        // then
        assertEquals(200, result.getStatusCode().value());
        assertTrue(result.getBody().success());
        assertNotNull(result.getBody().data());
        assertEquals(1, result.getBody().data().size());

        verify(newsService, times(1)).getNewsHistory(stockCode, start, end);
    }

    @Test
    @DisplayName("날짜 파라미터 없이 이력 조회 시 기본값(1개월)을 사용한다")
    void getNewsHistory_DefaultDates() {
        // given
        String stockCode = "000020";
        LocalDateTime now = LocalDateTime.now();

        when(newsService.getNewsHistory(anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());

        // when
        newsController.getNewsHistory(stockCode, null, null);

        // then
        verify(newsService, times(1)).getNewsHistory(
                eq(stockCode),
                argThat(date -> date != null && date.isBefore(now)),
                argThat(date -> date != null && date.isAfter(now.minusDays(1)))
        );
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (closeable != null) {
            closeable.close();
        }
    }
}
