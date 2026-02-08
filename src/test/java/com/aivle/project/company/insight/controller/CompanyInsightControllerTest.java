package com.aivle.project.company.insight.controller;

import com.aivle.project.common.dto.ApiResponse;
import com.aivle.project.company.insight.dto.CompanyInsightDto;
import com.aivle.project.company.insight.dto.CompanyInsightType;
import com.aivle.project.company.insight.service.CompanyInsightService;
import java.time.LocalDateTime;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ㅋCompanyInsightControllerTest {

	@Mock
	private CompanyInsightService companyInsightService;

	@InjectMocks
	private CompanyInsightController companyInsightController;

	private AutoCloseable closeable;

	@BeforeEach
	void setUp() {
		closeable = MockitoAnnotations.openMocks(this);
	}

	@Test
	@DisplayName("기업 인사이트를 성공적으로 조회한다")
	void getCompanyInsights_Success() {
		// given
		Long companyId = 100L;
		List<CompanyInsightDto> items = List.of(
			CompanyInsightDto.builder()
				.id(1L)
				.type(CompanyInsightType.REPORT)
				.title("보고서")
				.body("요약")
				.content(null)
				.source(null)
				.publishedAt(LocalDateTime.of(2026, 2, 6, 0, 0))
				.url(null)
				.build(),
			CompanyInsightDto.builder()
				.id(2L)
				.type(CompanyInsightType.NEWS)
				.title("뉴스")
				.body(null)
				.content("본문")
				.source("NEU")
				.publishedAt(LocalDateTime.of(2026, 2, 6, 0, 0))
				.url("https://example.com")
				.build()
		);

		when(companyInsightService.getInsights(eq(companyId), anyInt(), anyInt(), anyInt(), anyInt()))
			.thenReturn(new com.aivle.project.company.insight.service.CompanyInsightService.InsightResult(items, false));

		// when
		ResponseEntity<ApiResponse<List<CompanyInsightDto>>> result = companyInsightController
			.getCompanyInsights(companyId, null, null, null, null);

		// then
		assertEquals(200, result.getStatusCode().value());
		assertTrue(result.getBody().success());
		assertNotNull(result.getBody().data());
		assertEquals(2, result.getBody().data().size());

		verify(companyInsightService, times(1)).getInsights(eq(companyId), anyInt(), anyInt(), anyInt(), anyInt());
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		if (closeable != null) {
			closeable.close();
		}
	}
}
