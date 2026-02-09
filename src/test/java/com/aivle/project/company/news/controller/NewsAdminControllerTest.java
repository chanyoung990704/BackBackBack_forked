package com.aivle.project.company.news.controller;

import com.aivle.project.common.dto.ApiResponse;
import com.aivle.project.company.news.dto.NewsAnalysisResponse;
import com.aivle.project.company.news.dto.NewsRefreshResponse;
import com.aivle.project.company.news.service.NewsService;
import com.aivle.project.company.service.CompanyReputationScoreService;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NewsAdminControllerTest {

	@Mock
	private NewsService newsService;

	@Mock
	private CompanyReputationScoreService companyReputationScoreService;

	@InjectMocks
	private NewsAdminController newsAdminController;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
		@DisplayName("최신 뉴스 재수집 API가 성공하고 연결 점수를 동기화한다")
	void refreshLatestNews_Success() {
		// given
		String stockCode = "000020";
		NewsAnalysisResponse analysis = new NewsAnalysisResponse(
			1L,
			100L,
			"동화약품",
			2,
			0.125,
			OffsetDateTime.now(),
			List.of(),
			OffsetDateTime.now()
		);
		NewsRefreshResponse response = new NewsRefreshResponse(analysis, true);
		when(newsService.refreshLatestNews(stockCode)).thenReturn(response);

		// when
		ResponseEntity<ApiResponse<NewsRefreshResponse>> result = newsAdminController.refreshLatestNews(stockCode);

		// then
		assertEquals(200, result.getStatusCode().value());
		assertTrue(result.getBody().success());
			assertNotNull(result.getBody().data());
			assertTrue(result.getBody().data().averageScoreRepaired());
			verify(newsService, times(1)).refreshLatestNews(stockCode);
			verify(companyReputationScoreService, times(1))
				.syncExternalHealthScoreIfPresent(100L, stockCode);
	}
}
