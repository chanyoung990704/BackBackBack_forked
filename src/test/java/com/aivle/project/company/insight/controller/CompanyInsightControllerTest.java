package com.aivle.project.company.insight.controller;

import com.aivle.project.common.dto.ApiResponse;
import com.aivle.project.company.insight.dto.CompanyInsightItem;
import com.aivle.project.company.insight.dto.CompanyInsightType;
import com.aivle.project.company.insight.service.CompanyInsightService;
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
		List<CompanyInsightItem> items = List.of(
			new CompanyInsightItem(1L, CompanyInsightType.REPORT, "보고서", null, "요약", null, "2026-02-06T00:00:00Z", null),
			new CompanyInsightItem(2L, CompanyInsightType.NEWS, "뉴스", "본문", null, "NEU", "2026-02-06T00:00:00Z", "https://example.com")
		);

		when(companyInsightService.getInsights(eq(companyId), anyInt(), anyInt(), anyInt(), anyInt()))
			.thenReturn(new com.aivle.project.company.insight.service.CompanyInsightService.InsightResult(items, false));

		// when
		ResponseEntity<ApiResponse<List<CompanyInsightItem>>> result = companyInsightController
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
