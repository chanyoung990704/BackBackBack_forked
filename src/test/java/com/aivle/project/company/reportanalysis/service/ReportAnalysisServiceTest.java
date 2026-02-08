package com.aivle.project.company.reportanalysis.service;

import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.news.client.NewsClient;
import com.aivle.project.company.news.dto.NewsItemResponse;
import com.aivle.project.company.reportanalysis.dto.ReportApiResponse;
import com.aivle.project.company.reportanalysis.entity.ReportAnalysisEntity;
import com.aivle.project.company.reportanalysis.entity.ReportContentEntity;
import com.aivle.project.company.reportanalysis.repository.ReportAnalysisRepository;
import com.aivle.project.company.reportanalysis.repository.ReportContentRepository;
import com.aivle.project.company.repository.CompaniesRepository;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportAnalysisServiceTest {

	@Mock
	private ReportAnalysisRepository reportAnalysisRepository;

	@Mock
	private ReportContentRepository reportContentRepository;

	@Mock
	private NewsClient newsClient;

	@Mock
	private CompaniesRepository companiesRepository;

	@InjectMocks
	private ReportAnalysisService reportAnalysisService;

	private AutoCloseable closeable;

	@BeforeEach
	void setUp() {
		closeable = MockitoAnnotations.openMocks(this);
	}

	@Test
	@DisplayName("사업보고서 응답이 비어있으면 placeholder를 저장한다")
	void fetchAndStoreReport_EmptyContents() {
		// given
		String stockCode = "000020";
		String companyName = "동화약품";
		CompaniesEntity company = CompaniesEntity.create("000020", companyName, null, stockCode, null);
		setId(company, 1L);

		ReportApiResponse apiResponse = new ReportApiResponse(
			companyName,
			0,
			List.<NewsItemResponse>of(),
			null,
			"2026-02-07T09:03:59.304242"
		);

		when(companiesRepository.findByStockCode(stockCode)).thenReturn(Optional.of(company));
		when(newsClient.fetchReport(stockCode)).thenReturn(apiResponse);
		when(reportAnalysisRepository.save(any())).thenAnswer(invocation -> {
			ReportAnalysisEntity saved = invocation.getArgument(0);
			setId(saved, 10L);
			setCreatedAt(saved);
			return saved;
		});
		when(reportContentRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

		// when
		var result = reportAnalysisService.fetchAndStoreReport(stockCode);

		// then
		assertNotNull(result);
		ArgumentCaptor<List<ReportContentEntity>> captor = ArgumentCaptor.forClass(List.class);
		verify(reportContentRepository, times(1)).saveAll(captor.capture());
		List<ReportContentEntity> savedContents = captor.getValue();
		assertEquals(1, savedContents.size());
		assertEquals("요약 데이터 없음", savedContents.get(0).getTitle());
		assertEquals("요약 데이터가 제공되지 않았습니다.", savedContents.get(0).getSummary());
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
