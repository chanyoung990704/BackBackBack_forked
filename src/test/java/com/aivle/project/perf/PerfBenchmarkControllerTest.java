package com.aivle.project.perf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aivle.project.common.dto.ApiResponse;
import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.insight.service.CompanyInsightService;
import com.aivle.project.company.news.service.NewsService;
import com.aivle.project.company.reportanalysis.service.ReportAnalysisService;
import com.aivle.project.company.repository.CompaniesRepository;
import com.aivle.project.company.service.CompanyAiService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PerfBenchmarkControllerTest {

	@Mock
	private CompaniesRepository companiesRepository;

	@Mock
	private NewsService newsService;

	@Mock
	private ReportAnalysisService reportAnalysisService;

	@Mock
	private CompanyInsightService companyInsightService;

	@Mock
	private CompanyAiService companyAiService;

	@InjectMocks
	private PerfBenchmarkController controller;

	@Test
	@DisplayName("fixture 조회 시 companyId와 stockCode를 반환한다")
	void fixture_shouldReturnCompanyInfo() {
		// given
		CompaniesEntity company = CompaniesEntity.create(
			"90000001",
			"PERF_MOCK_COMPANY",
			"PERF MOCK COMPANY",
			PerfDataInitializer.PERF_STOCK_CODE,
			LocalDate.now()
		);
		ReflectionTestUtils.setField(company, "id", 1L);
		when(companiesRepository.findByStockCode(PerfDataInitializer.PERF_STOCK_CODE))
			.thenReturn(Optional.of(company));

		// when
		ResponseEntity<ApiResponse<java.util.Map<String, Object>>> response = controller.fixture();

		// then
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().success()).isTrue();
		assertThat(response.getBody().data()).containsEntry("companyId", 1L);
		assertThat(response.getBody().data()).containsEntry("stockCode", PerfDataInitializer.PERF_STOCK_CODE);
	}

	@Test
	@DisplayName("insight-refresh 호출 시 인사이트 서비스가 refresh=true로 실행된다")
	void insightRefresh_shouldDelegateToInsightService() {
		// given
		when(companyInsightService.getInsights(1L, 0, 10, 0, 1, true))
			.thenReturn(new CompanyInsightService.InsightResult(List.of(), null, false));

		// when
		ResponseEntity<ApiResponse<java.util.Map<String, Object>>> response = controller.insightRefresh(1L);

		// then
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().success()).isTrue();
		verify(companyInsightService).getInsights(1L, 0, 10, 0, 1, true);
	}

	@Test
	@DisplayName("ai-report 호출 시 리포트 생성 서비스가 실행된다")
	void aiReport_shouldDelegateToAiService() {
		// given
		when(companyAiService.generateAndSaveReport(1L, 2026, 1)).thenReturn(mock(com.aivle.project.file.entity.FilesEntity.class));

		// when
		ResponseEntity<ApiResponse<java.util.Map<String, Object>>> response = controller.aiReport(1L, 2026, 1);

		// then
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().success()).isTrue();
		verify(companyAiService).generateAndSaveReport(1L, 2026, 1);
	}
}
