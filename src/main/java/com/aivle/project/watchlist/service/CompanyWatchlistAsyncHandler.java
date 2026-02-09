package com.aivle.project.watchlist.service;

import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.repository.CompaniesRepository;
import com.aivle.project.company.service.CompanyHealthScoreCacheService;
import com.aivle.project.company.service.CompanyAiCommentService;
import com.aivle.project.company.service.CompanyPredictionCacheService;
import com.aivle.project.company.service.CompanyReputationScoreService;
import com.aivle.project.company.service.CompanySignalCacheService;
import com.aivle.project.report.repository.CompanyReportMetricValuesRepository;
import com.aivle.project.watchlist.event.CompanyWatchlistCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
	@RequiredArgsConstructor
public class CompanyWatchlistAsyncHandler {

	private final CompaniesRepository companiesRepository;
	private final CompanyReportMetricValuesRepository companyReportMetricValuesRepository;
	private final CompanyHealthScoreCacheService companyHealthScoreCacheService;
	private final CompanyAiCommentService companyAiCommentService;
	private final CompanyPredictionCacheService companyPredictionCacheService;
	private final CompanySignalCacheService companySignalCacheService;
	private final CompanyReputationScoreService companyReputationScoreService;

	@Async("insightExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleWatchlistCreated(CompanyWatchlistCreatedEvent event) {
		try {
			CompaniesEntity company = companiesRepository.findById(event.companyId()).orElse(null);
			if (company == null) {
				return;
			}
			String stockCode = company.getStockCode();
			Integer latestActualQuarterKey = companyReportMetricValuesRepository
				.findMaxActualQuarterKeyByStockCode(stockCode)
				.orElse(null);
			if (latestActualQuarterKey == null) {
				return;
			}
			// 워치리스트 등록 후 기업 개요 캐시를 비동기로 선행 적재한다.
			companyHealthScoreCacheService.ensureHealthScoreCached(event.companyId(), latestActualQuarterKey);
			companyPredictionCacheService.ensurePredictionCached(event.companyId(), latestActualQuarterKey);
			companySignalCacheService.ensureSignalsCached(event.companyId(), latestActualQuarterKey);
			companyReputationScoreService.syncExternalHealthScoreIfPresent(event.companyId(), stockCode);
			companyAiCommentService.ensureAiCommentCached(event.companyId(), String.valueOf(latestActualQuarterKey));
		} catch (Exception e) {
			log.warn("워치리스트 등록 후 기업 정보 캐시 실패: companyId={}", event.companyId(), e);
		}
	}
}
