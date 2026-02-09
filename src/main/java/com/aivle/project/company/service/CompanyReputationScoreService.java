package com.aivle.project.company.service;

import com.aivle.project.company.keymetric.entity.CompanyKeyMetricEntity;
import com.aivle.project.company.keymetric.repository.CompanyKeyMetricRepository;
import com.aivle.project.company.news.entity.NewsAnalysisEntity;
import com.aivle.project.company.news.repository.NewsAnalysisRepository;
import com.aivle.project.company.news.service.NewsService;
import com.aivle.project.quarter.entity.QuartersEntity;
import com.aivle.project.quarter.repository.QuartersRepository;
import com.aivle.project.report.repository.CompanyReportMetricValuesRepository;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 외부 건강도(평판) 점수 동기화 서비스.
 */
@Service
@RequiredArgsConstructor
public class CompanyReputationScoreService {

	private final CompanyReportMetricValuesRepository companyReportMetricValuesRepository;
	private final QuartersRepository quartersRepository;
	private final CompanyKeyMetricRepository companyKeyMetricRepository;
	private final NewsAnalysisRepository newsAnalysisRepository;
	private final NewsService newsService;

	@Transactional
	public void syncExternalHealthScoreIfPresent(Long companyId, String stockCode) {
		Integer latestActualQuarterKey = companyReportMetricValuesRepository.findMaxActualQuarterKeyByStockCode(stockCode)
			.orElse(null);
		if (latestActualQuarterKey == null) {
			return;
		}

		QuartersEntity quarter = quartersRepository.findByQuarterKey(latestActualQuarterKey)
			.orElse(null);
		if (quarter == null) {
			return;
		}

		CompanyKeyMetricEntity keyMetric = companyKeyMetricRepository
			.findByCompanyIdAndQuarterId(companyId, quarter.getId())
			.orElse(null);
		if (keyMetric == null) {
			return;
		}

		BigDecimal averageScore = resolveLatestAverageScore(companyId, stockCode, true);
		if (averageScore == null) {
			return;
		}

		keyMetric.applyExternalHealthScore(averageScore);
	}

	@Transactional(readOnly = true)
	public BigDecimal resolveLatestAverageScore(Long companyId, String stockCode) {
		return resolveLatestAverageScore(companyId, stockCode, false);
	}

	private BigDecimal resolveLatestAverageScore(Long companyId, String stockCode, boolean fetchIfMissing) {
		Optional<NewsAnalysisEntity> latest = newsAnalysisRepository
			.findTopByCompanyIdOrderByAnalyzedAtDesc(companyId);
		if (latest.isEmpty() && fetchIfMissing) {
			newsService.fetchAndStoreNews(stockCode);
			latest = newsAnalysisRepository.findTopByCompanyIdOrderByAnalyzedAtDesc(companyId);
		}
		return latest.map(NewsAnalysisEntity::getAverageScore).orElse(null);
	}
}
