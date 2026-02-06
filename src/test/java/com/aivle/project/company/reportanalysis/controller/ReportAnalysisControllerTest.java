package com.aivle.project.company.reportanalysis.controller;

import com.aivle.project.common.dto.ApiResponse;
import com.aivle.project.company.reportanalysis.dto.ReportAnalysisResponse;
import com.aivle.project.company.reportanalysis.service.ReportAnalysisService;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportAnalysisControllerTest {

	@Mock
	private ReportAnalysisService reportAnalysisService;

	@InjectMocks
	private ReportAnalysisController reportAnalysisController;

	private AutoCloseable closeable;

	@BeforeEach
	void setUp() {
		closeable = MockitoAnnotations.openMocks(this);
	}

	@Test
	@DisplayName("사업보고서 분석 데이터를 성공적으로 수집한다")
	void fetchReportAnalysis_Success() {
		// given
		String stockCode = "000020";
		String companyName = "동화약품";

		ReportAnalysisResponse response = new ReportAnalysisResponse(
			1L,
			100L,
			companyName,
			1,
			0.0041,
			OffsetDateTime.now(),
			List.of(),
			OffsetDateTime.now()
		);

		when(reportAnalysisService.fetchAndStoreReport(stockCode)).thenReturn(response);

		// when
		ResponseEntity<ApiResponse<ReportAnalysisResponse>> result = reportAnalysisController.fetchReportAnalysis(stockCode);

		// then
		assertEquals(200, result.getStatusCode().value());
		assertTrue(result.getBody().success());
		assertNotNull(result.getBody().data());
		assertEquals(companyName, result.getBody().data().companyName());

		verify(reportAnalysisService, times(1)).fetchAndStoreReport(stockCode);
	}

	@Test
	@DisplayName("최신 사업보고서 분석을 성공적으로 조회한다")
	void getLatestReportAnalysis_Success() {
		// given
		String stockCode = "000020";
		String companyName = "동화약품";

		ReportAnalysisResponse response = new ReportAnalysisResponse(
			1L,
			100L,
			companyName,
			1,
			0.0041,
			OffsetDateTime.now(),
			List.of(),
			OffsetDateTime.now()
		);

		when(reportAnalysisService.getLatestReport(stockCode)).thenReturn(response);

		// when
		ResponseEntity<ApiResponse<ReportAnalysisResponse>> result = reportAnalysisController.getLatestReportAnalysis(stockCode);

		// then
		assertEquals(200, result.getStatusCode().value());
		assertTrue(result.getBody().success());
		assertNotNull(result.getBody().data());
		assertEquals(companyName, result.getBody().data().companyName());

		verify(reportAnalysisService, times(1)).getLatestReport(stockCode);
	}

	@Test
	@DisplayName("사업보고서 분석 이력이 없는 경우 에러 응답을 반환한다")
	void getLatestReportAnalysis_NoData() {
		// given
		String stockCode = "000020";
		when(reportAnalysisService.getLatestReport(stockCode)).thenReturn(null);

		// when
		ResponseEntity<ApiResponse<ReportAnalysisResponse>> result = reportAnalysisController.getLatestReportAnalysis(stockCode);

		// then
		assertEquals(200, result.getStatusCode().value());
		assertFalse(result.getBody().success());
		assertNull(result.getBody().data());
		assertNotNull(result.getBody().error());

		verify(reportAnalysisService, times(1)).getLatestReport(stockCode);
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		if (closeable != null) {
			closeable.close();
		}
	}
}
