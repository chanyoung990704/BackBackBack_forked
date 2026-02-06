package com.aivle.project.watchlist.service;

import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.repository.CompaniesRepository;
import com.aivle.project.common.error.CommonException;
import com.aivle.project.metric.entity.MetricValueType;
import com.aivle.project.quarter.entity.QuartersEntity;
import com.aivle.project.quarter.repository.QuartersRepository;
import com.aivle.project.risk.entity.RiskLevel;
import com.aivle.project.user.entity.UserEntity;
import com.aivle.project.user.repository.UserRepository;
import com.aivle.project.watchlist.dto.WatchlistDashboardMetricRow;
import com.aivle.project.watchlist.dto.WatchlistDashboardResponse;
import com.aivle.project.watchlist.dto.WatchlistDashboardRiskRow;
import com.aivle.project.watchlist.dto.WatchlistMetricAverageRow;
import com.aivle.project.watchlist.dto.WatchlistMetricAveragesResponse;
import com.aivle.project.watchlist.entity.CompanyWatchlistEntity;
import com.aivle.project.watchlist.error.WatchlistErrorCode;
import com.aivle.project.watchlist.repository.CompanyWatchlistRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompanyWatchlistService {

	private final CompanyWatchlistRepository companyWatchlistRepository;
	private final UserRepository userRepository;
	private final CompaniesRepository companiesRepository;
	private final QuartersRepository quartersRepository;

	@Transactional
	public void addWatchlist(Long userId, Long companyId, String note) {
		CompanyWatchlistEntity existing = companyWatchlistRepository.findByUserIdAndCompanyId(userId, companyId).orElse(null);
		if (existing != null) {
			throw new CommonException(WatchlistErrorCode.WATCHLIST_DUPLICATE);
		}
		UserEntity user = userRepository.getReferenceById(userId);
		CompaniesEntity company = companiesRepository.getReferenceById(companyId);
		companyWatchlistRepository.save(CompanyWatchlistEntity.create(user, company, note));
	}

	@Transactional
	public void removeWatchlist(Long userId, Long companyId) {
		CompanyWatchlistEntity existing = companyWatchlistRepository.findByUserIdAndCompanyId(userId, companyId)
			.orElseThrow(() -> new CommonException(WatchlistErrorCode.WATCHLIST_NOT_FOUND));
		if (!existing.getUser().getId().equals(userId)) {
			throw new CommonException(WatchlistErrorCode.WATCHLIST_FORBIDDEN);
		}
		existing.delete();
	}

	@Transactional(readOnly = true)
	public WatchlistDashboardResponse getDashboard(
		Long userId,
		int year,
		int quarter,
		List<String> metricCodes,
		RiskLevel riskLevel
	) {
		QuartersEntity quarterEntity = quartersRepository.findByYearAndQuarter((short) year, (byte) quarter)
			.orElseThrow(() -> new IllegalArgumentException("유효하지 않은 분기입니다."));
		List<String> codes = metricCodes == null ? List.of() : metricCodes.stream().map(String::trim).filter(s -> !s.isBlank()).toList();
		boolean emptyCodes = codes.isEmpty();

		List<WatchlistDashboardMetricRow> metrics = companyWatchlistRepository.findDashboardMetrics(
			userId,
			(short) year,
			(byte) quarter,
			MetricValueType.ACTUAL,
			emptyCodes ? List.of("__none__") : codes,
			emptyCodes
		).stream().map(p -> new WatchlistDashboardMetricRow(
			p.getWatchlistId(), p.getCompanyId(), p.getCorpName(), p.getCorpCode(), p.getMetricCode(), p.getMetricNameKo(),
			p.getMetricValue(), p.getMarketAvg(), p.getMarketN()
		)).toList();

		List<WatchlistDashboardRiskRow> risks = companyWatchlistRepository.findDashboardRisks(
			userId,
			quarterEntity.getId(),
			riskLevel
		).stream().map(p -> new WatchlistDashboardRiskRow(
			p.getWatchlistId(), p.getCompanyId(), p.getCorpName(), p.getRiskScore(), p.getRiskLevel(), p.getRiskMetricsAvg(), p.getLastRefreshedAt()
		)).toList();

		return new WatchlistDashboardResponse(year, quarter, metrics, risks);
	}

	@Transactional(readOnly = true)
	public WatchlistMetricAveragesResponse getWatchlistMetricAverages(Long userId, int year, int quarter, List<String> metricCodes) {
		quartersRepository.findByYearAndQuarter((short) year, (byte) quarter)
			.orElseThrow(() -> new IllegalArgumentException("유효하지 않은 분기입니다."));
		List<String> codes = metricCodes == null ? List.of() : metricCodes.stream().map(String::trim).filter(s -> !s.isBlank()).toList();
		boolean emptyCodes = codes.isEmpty();

		List<WatchlistMetricAverageRow> metrics = companyWatchlistRepository.findWatchlistMetricAverages(
			userId,
			(short) year,
			(byte) quarter,
			MetricValueType.ACTUAL,
			emptyCodes ? List.of("__none__") : codes,
			emptyCodes
		).stream().map(p -> new WatchlistMetricAverageRow(
			p.getMetricCode(),
			p.getMetricNameKo(),
			p.getAvgValue(),
			p.getSampleCompanyCount()
		)).toList();

		return new WatchlistMetricAveragesResponse(year, quarter, metrics);
	}
}
