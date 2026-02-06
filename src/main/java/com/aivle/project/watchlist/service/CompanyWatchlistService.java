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
import com.aivle.project.watchlist.dto.WatchlistMetricValueRow;
import com.aivle.project.watchlist.dto.WatchlistMetricValuesResponse;
import com.aivle.project.watchlist.dto.WatchlistQuarterMetricValues;
import com.aivle.project.watchlist.entity.CompanyWatchlistEntity;
import com.aivle.project.watchlist.error.WatchlistErrorCode;
import com.aivle.project.watchlist.repository.CompanyWatchlistRepository;
import com.aivle.project.watchlist.repository.WatchlistMetricValueProjection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

	@Transactional(readOnly = true)
	public WatchlistMetricValuesResponse getWatchlistMetricValuesByQuarter(Long userId, int year, int quarter) {
		quartersRepository.findByYearAndQuarter((short) year, (byte) quarter)
			.orElseThrow(() -> new IllegalArgumentException("유효하지 않은 분기입니다."));

		List<WatchlistMetricValueProjection> rows = companyWatchlistRepository.findWatchlistMetricValues(
			userId,
			(short) year,
			(byte) quarter,
			MetricValueType.ACTUAL
		);

		return new WatchlistMetricValuesResponse(groupByQuarter(rows));
	}

	@Transactional(readOnly = true)
	public WatchlistMetricValuesResponse getWatchlistMetricValuesByQuarterRange(
		Long userId,
		int fromYear,
		int fromQuarter,
		int toYear,
		int toQuarter
	) {
		if (!isValidQuarterRange(fromYear, fromQuarter, toYear, toQuarter)) {
			throw new IllegalArgumentException("유효하지 않은 분기 범위입니다.");
		}

		List<WatchlistMetricValueProjection> rows = companyWatchlistRepository.findWatchlistMetricValuesInRange(
			userId,
			(short) fromYear,
			(byte) fromQuarter,
			(short) toYear,
			(byte) toQuarter,
			MetricValueType.ACTUAL
		);

		return new WatchlistMetricValuesResponse(groupByQuarter(rows));
	}

	private boolean isValidQuarterRange(int fromYear, int fromQuarter, int toYear, int toQuarter) {
		if (fromQuarter < 1 || fromQuarter > 4 || toQuarter < 1 || toQuarter > 4) {
			return false;
		}
		if (fromYear > toYear) {
			return false;
		}
		return fromYear != toYear || fromQuarter <= toQuarter;
	}

	private List<WatchlistQuarterMetricValues> groupByQuarter(List<WatchlistMetricValueProjection> rows) {
		Map<String, WatchlistQuarterMetricValues> grouped = new LinkedHashMap<>();
		for (WatchlistMetricValueProjection row : rows) {
			String key = row.getYear() + "-" + row.getQuarter();
			WatchlistQuarterMetricValues bucket = grouped.get(key);
			if (bucket == null) {
				bucket = new WatchlistQuarterMetricValues(row.getYear(), row.getQuarter(), new ArrayList<>());
				grouped.put(key, bucket);
			}
			bucket.items().add(new WatchlistMetricValueRow(
				row.getWatchlistId(),
				row.getCompanyId(),
				row.getCorpName(),
				row.getCorpCode(),
				row.getMetricCode(),
				row.getMetricNameKo(),
				row.getMetricValue()
			));
		}
		return new ArrayList<>(grouped.values());
	}
}
