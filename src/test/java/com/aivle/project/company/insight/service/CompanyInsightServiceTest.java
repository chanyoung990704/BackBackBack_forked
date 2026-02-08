package com.aivle.project.company.insight.service;

import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.insight.dto.CompanyInsightDto;
import com.aivle.project.company.insight.dto.CompanyInsightType;
import com.aivle.project.company.news.entity.NewsAnalysisEntity;
import com.aivle.project.company.news.entity.NewsArticleEntity;
import com.aivle.project.company.news.repository.NewsAnalysisRepository;
import com.aivle.project.company.news.repository.NewsArticleRepository;
import com.aivle.project.company.news.service.NewsService;
import com.aivle.project.company.reportanalysis.entity.ReportAnalysisEntity;
import com.aivle.project.company.reportanalysis.entity.ReportContentEntity;
import com.aivle.project.company.reportanalysis.repository.ReportAnalysisRepository;
import com.aivle.project.company.reportanalysis.repository.ReportContentRepository;
import com.aivle.project.company.reportanalysis.service.ReportAnalysisService;
import com.aivle.project.company.repository.CompaniesRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyInsightServiceTest {

	@Mock
	private CompaniesRepository companiesRepository;
	@Mock
	private NewsAnalysisRepository newsAnalysisRepository;
	@Mock
	private NewsArticleRepository newsArticleRepository;
	@Mock
	private ReportAnalysisRepository reportAnalysisRepository;
	@Mock
	private ReportContentRepository reportContentRepository;
	@Mock
	private NewsService newsService;
	@Mock
	private ReportAnalysisService reportAnalysisService;

	@InjectMocks
	private CompanyInsightService companyInsightService;

	@Test
	@DisplayName("DB에 인사이트가 있으면 DB 데이터를 반환한다")
	void getInsights_ReturnsDatabaseItems() {
		// given
		Long companyId = 1L;
		CompaniesEntity company = CompaniesEntity.create("001", "테스트기업", "TEST", "123456", LocalDate.now());
		ReportAnalysisEntity reportAnalysis = ReportAnalysisEntity.create(company, "테스트기업", 1, BigDecimal.ONE, LocalDateTime.now());
		NewsAnalysisEntity newsAnalysis = NewsAnalysisEntity.create(company, "테스트기업", 1, BigDecimal.ONE, LocalDateTime.now());

		ReportContentEntity reportContent = ReportContentEntity.create(
			reportAnalysis, "보고서", "요약", BigDecimal.ONE, LocalDateTime.of(2026, 2, 6, 0, 0), "https://r.example", null
		);
		NewsArticleEntity newsArticle = NewsArticleEntity.create(
			newsAnalysis, "뉴스", "본문", BigDecimal.ONE, LocalDateTime.of(2026, 2, 6, 0, 0), "https://n.example", "NEU"
		);

		when(companiesRepository.findById(companyId)).thenReturn(Optional.of(company));
		when(reportAnalysisRepository.findTopByCompanyIdOrderByAnalyzedAtDesc(companyId)).thenReturn(Optional.of(reportAnalysis));
		when(newsAnalysisRepository.findTopByCompanyIdOrderByAnalyzedAtDesc(companyId)).thenReturn(Optional.of(newsAnalysis));
		when(reportContentRepository.existsByReportAnalysisId(any())).thenReturn(true);
		when(newsArticleRepository.existsByNewsAnalysisId(any())).thenReturn(true);
		when(reportContentRepository.findByReportAnalysisIdOrderByPublishedAtDesc(eq(reportAnalysis.getId()), any(PageRequest.class)))
			.thenReturn(new PageImpl<>(List.of(reportContent)));
		when(newsArticleRepository.findByNewsAnalysisIdOrderByPublishedAtDesc(eq(newsAnalysis.getId()), any(PageRequest.class)))
			.thenReturn(new PageImpl<>(List.of(newsArticle)));

		// when
		CompanyInsightService.InsightResult result = companyInsightService.getInsights(companyId, 0, 1, 0, 1);

		// then
		assertThat(result.processing()).isFalse();
		assertThat(result.items()).hasSize(2);
		assertThat(result.items().get(0).getType()).isEqualTo(CompanyInsightType.REPORT);
		assertThat(result.items().get(1).getType()).isEqualTo(CompanyInsightType.NEWS);
		verify(reportAnalysisService, org.mockito.Mockito.never()).fetchAndStoreReport(any());
		verify(newsService, org.mockito.Mockito.never()).fetchAndStoreNews(any());
	}

	@Test
	@DisplayName("DB에 인사이트가 없으면 외부 API 결과를 반환한다")
	void getInsights_ReturnsExternalItems() {
		// given
		Long companyId = 1L;
		CompaniesEntity company = CompaniesEntity.create("001", "테스트기업", "TEST", "123456", LocalDate.now());
		when(companiesRepository.findById(companyId)).thenReturn(Optional.of(company));
		when(reportAnalysisRepository.findTopByCompanyIdOrderByAnalyzedAtDesc(companyId)).thenReturn(Optional.empty());
		when(newsAnalysisRepository.findTopByCompanyIdOrderByAnalyzedAtDesc(companyId)).thenReturn(Optional.empty());
		when(reportContentRepository.findTopByReportAnalysisCompanyIdOrderByPublishedAtDesc(companyId))
			.thenReturn(Optional.empty());
		when(newsArticleRepository.findTopByNewsAnalysisCompanyIdOrderByPublishedAtDesc(companyId))
			.thenReturn(Optional.empty());
		when(reportAnalysisService.fetchAndStoreReport("123456")).thenReturn(null);
		when(newsService.fetchAndStoreNews("123456")).thenReturn(null);

		ReportAnalysisEntity reportAnalysis = ReportAnalysisEntity.create(company, "테스트기업", 1, BigDecimal.ONE, LocalDateTime.now());
		NewsAnalysisEntity newsAnalysis = NewsAnalysisEntity.create(company, "테스트기업", 1, BigDecimal.ONE, LocalDateTime.now());
		ReportContentEntity reportContent = ReportContentEntity.create(
			reportAnalysis, "보고서", "요약", BigDecimal.ONE, LocalDateTime.of(2026, 2, 6, 0, 0), "https://r.example", null
		);
		NewsArticleEntity newsArticle = NewsArticleEntity.create(
			newsAnalysis, "뉴스", "본문", BigDecimal.ONE, LocalDateTime.of(2026, 2, 6, 0, 0), "https://n.example", "NEU"
		);
		when(reportAnalysisRepository.findTopByCompanyIdOrderByAnalyzedAtDesc(companyId)).thenReturn(Optional.of(reportAnalysis));
		when(newsAnalysisRepository.findTopByCompanyIdOrderByAnalyzedAtDesc(companyId)).thenReturn(Optional.of(newsAnalysis));
		when(reportContentRepository.findByReportAnalysisIdOrderByPublishedAtDesc(eq(reportAnalysis.getId()), any(PageRequest.class)))
			.thenReturn(new PageImpl<>(List.of(reportContent)));
		when(newsArticleRepository.findByNewsAnalysisIdOrderByPublishedAtDesc(eq(newsAnalysis.getId()), any(PageRequest.class)))
			.thenReturn(new PageImpl<>(List.of(newsArticle)));

		// when
		CompanyInsightService.InsightResult result = companyInsightService.getInsights(companyId, 0, 1, 0, 1);

		// then
		assertThat(result.processing()).isFalse();
		assertThat(result.items()).hasSize(2);
		assertThat(result.items())
			.extracting(CompanyInsightDto::getType)
			.containsExactly(CompanyInsightType.REPORT, CompanyInsightType.NEWS);
		assertThat(result.items().get(0).getPublishedAt()).isNotNull();
		assertThat(result.items().get(1).getPublishedAt()).isNotNull();
		verify(reportAnalysisService).fetchAndStoreReport("123456");
		verify(newsService).fetchAndStoreNews("123456");
	}
}
