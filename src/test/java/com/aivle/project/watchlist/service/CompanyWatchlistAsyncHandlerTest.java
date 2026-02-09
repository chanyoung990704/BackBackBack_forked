package com.aivle.project.watchlist.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.repository.CompaniesRepository;
import com.aivle.project.company.service.CompanyAiCommentService;
import com.aivle.project.company.service.CompanyHealthScoreCacheService;
import com.aivle.project.company.service.CompanyPredictionCacheService;
import com.aivle.project.company.service.CompanyReputationScoreService;
import com.aivle.project.company.service.CompanySignalCacheService;
import com.aivle.project.report.repository.CompanyReportMetricValuesRepository;
import com.aivle.project.watchlist.event.CompanyWatchlistCreatedEvent;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompanyWatchlistAsyncHandlerTest {

	@Mock
	private CompaniesRepository companiesRepository;

	@Mock
	private CompanyReportMetricValuesRepository companyReportMetricValuesRepository;

	@Mock
	private CompanyHealthScoreCacheService companyHealthScoreCacheService;

	@Mock
	private CompanyAiCommentService companyAiCommentService;

	@Mock
	private CompanyPredictionCacheService companyPredictionCacheService;

	@Mock
	private CompanySignalCacheService companySignalCacheService;

	@Mock
	private CompanyReputationScoreService companyReputationScoreService;

	@InjectMocks
	private CompanyWatchlistAsyncHandler companyWatchlistAsyncHandler;

	@Test
	@DisplayName("워치리스트 등록 후 최신 ACTUAL 분기로 AI 코멘트를 선행 적재한다")
	void handleWatchlistCreated() {
		// given
		Long companyId = 10L;
		CompaniesEntity company = CompaniesEntity.create(
			"00000010",
			"테스트기업",
			"TEST_CO",
			"000020",
			LocalDate.of(2025, 1, 1)
		);
		when(companiesRepository.findById(companyId)).thenReturn(Optional.of(company));
		when(companyReportMetricValuesRepository.findMaxActualQuarterKeyByStockCode("000020"))
			.thenReturn(Optional.of(20253));

		// when
		companyWatchlistAsyncHandler.handleWatchlistCreated(new CompanyWatchlistCreatedEvent(1L, companyId));

		// then
		verify(companyHealthScoreCacheService).ensureHealthScoreCached(companyId, 20253);
		verify(companyPredictionCacheService).ensurePredictionCached(companyId, 20253);
		verify(companySignalCacheService).ensureSignalsCached(companyId, 20253);
		verify(companyReputationScoreService).syncExternalHealthScoreIfPresent(companyId, "000020");
		verify(companyAiCommentService).ensureAiCommentCached(companyId, "20253");
	}

	@Test
	@DisplayName("최신 ACTUAL 분기가 없으면 후속 캐시 적재를 수행하지 않는다")
	void skipWhenActualQuarterMissing() {
		// given
		Long companyId = 20L;
		CompaniesEntity company = CompaniesEntity.create(
			"00000020",
			"테스트기업2",
			"TEST_CO2",
			"000030",
			LocalDate.of(2025, 1, 1)
		);
		when(companiesRepository.findById(companyId)).thenReturn(Optional.of(company));
		when(companyReportMetricValuesRepository.findMaxActualQuarterKeyByStockCode("000030"))
			.thenReturn(Optional.empty());

		// when
		companyWatchlistAsyncHandler.handleWatchlistCreated(new CompanyWatchlistCreatedEvent(1L, companyId));

		// then
		verifyNoInteractions(companyHealthScoreCacheService);
		verifyNoInteractions(companyPredictionCacheService);
		verifyNoInteractions(companySignalCacheService);
		verifyNoInteractions(companyReputationScoreService);
		verifyNoInteractions(companyAiCommentService);
	}
}
