package com.aivle.project.dashboard.service;

import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.keymetric.entity.CompanyKeyMetricEntity;
import com.aivle.project.company.keymetric.entity.CompanyKeyMetricRiskLevel;
import com.aivle.project.company.keymetric.repository.CompanyKeyMetricRepository;
import com.aivle.project.dashboard.dto.CompanyQuarterRiskDto;
import com.aivle.project.dashboard.dto.DashboardSummaryResponse;
import com.aivle.project.dashboard.dto.KpiCardDto;
import com.aivle.project.dashboard.dto.MajorSectorDto;
import com.aivle.project.dashboard.dto.RiskStatusBucketDto;
import com.aivle.project.dashboard.dto.RiskStatusDistributionDto;
import com.aivle.project.dashboard.dto.RiskStatusDistributionPercentDto;
import com.aivle.project.quarter.support.QuarterCalculator;
import com.aivle.project.quarter.support.YearQuarter;
import com.aivle.project.report.repository.CompanyReportMetricValuesRepository;
import com.aivle.project.watchlist.entity.CompanyWatchlistEntity;
import com.aivle.project.watchlist.repository.CompanyWatchlistRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 대시보드 요약 조회 서비스.
 */
@Service
@RequiredArgsConstructor
public class DashboardSummaryService {

	private static final String RANGE_LABEL = "최근 4분기";

	private final CompanyWatchlistRepository companyWatchlistRepository;
	private final CompanyReportMetricValuesRepository companyReportMetricValuesRepository;
	private final CompanyKeyMetricRepository companyKeyMetricRepository;

	@Transactional(readOnly = true)
	public DashboardSummaryResponse getSummary(Long userId) {
		List<CompanyWatchlistEntity> watchlists = companyWatchlistRepository.findActiveByUserId(userId);
		if (watchlists.isEmpty()) {
			return new DashboardSummaryResponse(
				RANGE_LABEL,
				buildKpis(
					0,
					new RiskStatusDistributionDto(0, 0, 0),
					0.0,
					0.0,
					new KpiCardDto(
						"RISK_DWELL_TIME",
						"리스크 체류 기간",
						0.0,
						"분기",
						KpiCardDto.KpiTone.DEFAULT,
						null,
						null,
						new KpiCardDto.KpiTooltipDto(
							"주의/위험 상태에 머무른 평균 기간(분기 수)",
							"낮을수록 리스크 구간에서 빠르게 회복합니다.",
							"체류 기간이 긴 기업을 우선 점검하세요."
						)
					)
				),
				null,
				null,
				List.of(),
				new RiskStatusDistributionDto(0, 0, 0),
				new RiskStatusDistributionPercentDto(0.0, 0.0, 0.0),
				0.0,
				null,
				List.of()
			);
		}

		List<Long> companyIds = watchlists.stream()
			.map(cw -> cw.getCompany().getId())
			.toList();

		int latestActualQuarterKey = resolveLatestActualQuarterKey(watchlists);
		int forecastQuarterKey = QuarterCalculator.offset(QuarterCalculator.parseQuarterKey(latestActualQuarterKey), 1)
			.toQuarterKey();

		List<Integer> windowQuarterKeys = buildWindowQuarterKeys(latestActualQuarterKey);
		List<String> windowQuarters = windowQuarterKeys.stream().map(this::toQuarterLabel).toList();

		RiskStatusDistributionDto currentDistribution = countDistribution(companyIds, latestActualQuarterKey, false);
		RiskStatusDistributionPercentDto distributionPercent = toDistributionPercent(currentDistribution);
		double networkStatus = calculateNetworkStatus(companyIds, latestActualQuarterKey);
		double averageRiskLevel = roundOneDecimal(100.0 - networkStatus);
		double riskIndex = calculateRiskIndex(distributionPercent);
		KpiCardDto riskDwellTimeKpi = buildRiskDwellTimeKpi(companyIds, latestActualQuarterKey);
		MajorSectorDto majorSector = calculateMajorSector(watchlists, companyIds, latestActualQuarterKey);
		List<RiskStatusBucketDto> trend = buildTrend(companyIds, windowQuarterKeys, latestActualQuarterKey);

		return new DashboardSummaryResponse(
			RANGE_LABEL,
			buildKpis(companyIds.size(), currentDistribution, networkStatus, riskIndex, riskDwellTimeKpi),
			toQuarterLabel(latestActualQuarterKey),
			toQuarterLabel(forecastQuarterKey),
			windowQuarters,
			currentDistribution,
			distributionPercent,
			averageRiskLevel,
			majorSector,
			trend
		);
	}

	@Transactional(readOnly = true)
	public List<CompanyQuarterRiskDto> getRiskRecords(Long userId, int limit) {
		List<CompanyWatchlistEntity> watchlists = companyWatchlistRepository.findActiveByUserId(userId);
		if (watchlists.isEmpty()) {
			return List.of();
		}

		List<Long> companyIds = watchlists.stream()
			.map(cw -> cw.getCompany().getId())
			.toList();

		int latestActualQuarterKey = resolveLatestActualQuarterKey(watchlists);

		return companyKeyMetricRepository.findByCompanyIdIn(companyIds).stream()
			.filter(metric -> metric.getQuarter().getQuarterKey() <= latestActualQuarterKey)
			.filter(metric -> metric.getRiskLevel() != null)
			.sorted(
				Comparator.comparing((CompanyKeyMetricEntity metric) -> metric.getQuarter().getQuarterKey()).reversed()
					.thenComparing(metric -> metric.getCompany().getId())
			)
			.limit(limit)
			.map(metric -> new CompanyQuarterRiskDto(
				String.valueOf(metric.getCompany().getId()),
				metric.getCompany().getCorpName(),
				toQuarterLabel(metric.getQuarter().getQuarterKey()),
				toRiskLevel(metric.getRiskLevel())
			))
			.toList();
	}

	private int resolveLatestActualQuarterKey(List<CompanyWatchlistEntity> watchlists) {
		return watchlists.stream()
			.map(CompanyWatchlistEntity::getCompany)
			.map(CompaniesEntity::getStockCode)
			.filter(stockCode -> stockCode != null && !stockCode.isBlank())
			.map(stockCode -> companyReportMetricValuesRepository.findMaxActualQuarterKeyByStockCode(stockCode).orElse(null))
			.filter(java.util.Objects::nonNull)
			.max(Comparator.naturalOrder())
			.orElseThrow(() -> new IllegalArgumentException("ACTUAL 분기 데이터를 찾을 수 없습니다."));
	}

	private List<Integer> buildWindowQuarterKeys(int latestActualQuarterKey) {
		YearQuarter latest = QuarterCalculator.parseQuarterKey(latestActualQuarterKey);
		List<Integer> keys = new ArrayList<>();
		keys.add(QuarterCalculator.offset(latest, -3).toQuarterKey());
		keys.add(QuarterCalculator.offset(latest, -2).toQuarterKey());
		keys.add(QuarterCalculator.offset(latest, -1).toQuarterKey());
		keys.add(latest.toQuarterKey());
		keys.add(QuarterCalculator.offset(latest, 1).toQuarterKey());
		return keys;
	}

	private List<RiskStatusBucketDto> buildTrend(List<Long> companyIds, List<Integer> windowQuarterKeys, int latestActualQuarterKey) {
		List<RiskStatusBucketDto> buckets = new ArrayList<>();
		for (int quarterKey : windowQuarterKeys) {
			boolean forecast = quarterKey > latestActualQuarterKey;
			RiskStatusDistributionDto distribution = countDistribution(companyIds, quarterKey, forecast);
			RiskStatusBucketDto.DataType dataType = forecast
				? RiskStatusBucketDto.DataType.FORECAST
				: RiskStatusBucketDto.DataType.ACTUAL;
			buckets.add(new RiskStatusBucketDto(
				toQuarterLabel(quarterKey),
				dataType,
				distribution.NORMAL(),
				distribution.CAUTION(),
				distribution.RISK()
			));
		}
		return buckets;
	}

	private double calculateNetworkStatus(List<Long> companyIds, int latestActualQuarterKey) {
		List<CompanyKeyMetricEntity> metrics = companyKeyMetricRepository
			.findByCompanyIdInAndQuarter_QuarterKey(companyIds, latestActualQuarterKey);

		double average = metrics.stream()
			.map(CompanyKeyMetricEntity::getInternalHealthScore)
			.filter(java.util.Objects::nonNull)
			.mapToDouble(BigDecimal::doubleValue)
			.average()
			.orElse(0.0);

		return BigDecimal.valueOf(average)
			.setScale(1, RoundingMode.HALF_UP)
			.doubleValue();
	}

	private RiskStatusDistributionDto countDistribution(List<Long> companyIds, int quarterKey, boolean forecast) {
		List<CompanyKeyMetricEntity> metrics = companyKeyMetricRepository
			.findByCompanyIdInAndQuarter_QuarterKey(companyIds, quarterKey);

		Map<CompanyKeyMetricRiskLevel, Integer> counter = new EnumMap<>(CompanyKeyMetricRiskLevel.class);
		for (CompanyKeyMetricEntity metric : metrics) {
			CompanyKeyMetricRiskLevel level = metric.getRiskLevel();
			if (level == null) {
				continue;
			}
			counter.put(level, counter.getOrDefault(level, 0) + 1);
		}

		return new RiskStatusDistributionDto(
			counter.getOrDefault(CompanyKeyMetricRiskLevel.SAFE, 0),
			counter.getOrDefault(CompanyKeyMetricRiskLevel.WARN, 0),
			counter.getOrDefault(CompanyKeyMetricRiskLevel.RISK, 0)
		);
	}

	private RiskStatusDistributionPercentDto toDistributionPercent(RiskStatusDistributionDto distribution) {
		int total = distribution.NORMAL() + distribution.CAUTION() + distribution.RISK();
		if (total == 0) {
			return new RiskStatusDistributionPercentDto(0.0, 0.0, 0.0);
		}
		return new RiskStatusDistributionPercentDto(
			roundOneDecimal(distribution.NORMAL() * 100.0 / total),
			roundOneDecimal(distribution.CAUTION() * 100.0 / total),
			roundOneDecimal(distribution.RISK() * 100.0 / total)
		);
	}

	private double calculateRiskIndex(RiskStatusDistributionPercentDto distributionPercent) {
		return roundOneDecimal(distributionPercent.CAUTION() * 0.5 + distributionPercent.RISK());
	}

	private MajorSectorDto calculateMajorSector(
		List<CompanyWatchlistEntity> watchlists,
		List<Long> companyIds,
		int latestActualQuarterKey
	) {
		Map<Long, String> sectorByCompanyId = new HashMap<>();
		for (CompanyWatchlistEntity watchlist : watchlists) {
			Long companyId = watchlist.getCompany().getId();
			String sectorName = watchlist.getCompany().getIndustryCode() == null
				? null
				: watchlist.getCompany().getIndustryCode().getIndustryName();
			sectorByCompanyId.put(companyId, (sectorName == null || sectorName.isBlank()) ? "미분류" : sectorName);
		}

		List<CompanyKeyMetricEntity> metrics = companyKeyMetricRepository
			.findByCompanyIdInAndQuarter_QuarterKey(companyIds, latestActualQuarterKey);

		Map<Long, CompanyKeyMetricEntity> actualMetricByCompanyId = new HashMap<>();
		for (CompanyKeyMetricEntity metric : metrics) {
			actualMetricByCompanyId.put(metric.getCompany().getId(), metric);
		}

		Map<String, SectorAggregate> aggregateBySector = new HashMap<>();
		for (Long companyId : companyIds) {
			CompanyKeyMetricEntity metric = actualMetricByCompanyId.get(companyId);
			if (metric == null || metric.getRiskLevel() == null) {
				continue;
			}
			String sectorName = sectorByCompanyId.getOrDefault(companyId, "미분류");
			SectorAggregate aggregate = aggregateBySector.computeIfAbsent(sectorName, key -> new SectorAggregate());
			aggregate.totalCount++;
			if (metric.getRiskLevel() == CompanyKeyMetricRiskLevel.WARN) {
				aggregate.warnCount++;
			}
			if (metric.getRiskLevel() == CompanyKeyMetricRiskLevel.RISK) {
				aggregate.riskCount++;
			}
		}

		return aggregateBySector.entrySet().stream()
			.filter(entry -> entry.getValue().warnCount + entry.getValue().riskCount > 0)
			.max((a, b) -> compareSectorAggregate(a.getValue(), b.getValue()))
			.map(entry -> {
				SectorAggregate aggregate = entry.getValue();
				int riskCompanyCount = aggregate.warnCount + aggregate.riskCount;
				double riskRatio = roundOneDecimal(riskCompanyCount * 100.0 / aggregate.totalCount);
				double riskIndex = roundOneDecimal((aggregate.warnCount * 0.5 + aggregate.riskCount) * 100.0 / aggregate.totalCount);
				return new MajorSectorDto(
					entry.getKey(),
					riskCompanyCount,
					aggregate.totalCount,
					riskRatio,
					riskIndex
				);
			})
			.orElse(null);
	}

	private int compareSectorAggregate(SectorAggregate a, SectorAggregate b) {
		int scoreA = a.warnCount + (a.riskCount * 2);
		int scoreB = b.warnCount + (b.riskCount * 2);
		if (scoreA != scoreB) {
			return Integer.compare(scoreA, scoreB);
		}
		int riskCountA = a.warnCount + a.riskCount;
		int riskCountB = b.warnCount + b.riskCount;
		if (riskCountA != riskCountB) {
			return Integer.compare(riskCountA, riskCountB);
		}
		return Integer.compare(a.totalCount, b.totalCount);
	}

	private KpiCardDto buildRiskDwellTimeKpi(List<Long> companyIds, int latestActualQuarterKey) {
		List<CompanyKeyMetricEntity> actualMetrics = companyKeyMetricRepository
			.findByCompanyIdIn(companyIds).stream()
			.filter(metric -> metric.getQuarter().getQuarterKey() <= latestActualQuarterKey)
			.toList();

		Map<Long, Map<Integer, CompanyKeyMetricRiskLevel>> riskHistoryByCompany = new HashMap<>();
		for (CompanyKeyMetricEntity metric : actualMetrics) {
			if (metric.getRiskLevel() == null) {
				continue;
			}
			riskHistoryByCompany
				.computeIfAbsent(metric.getCompany().getId(), key -> new HashMap<>())
				.put(metric.getQuarter().getQuarterKey(), metric.getRiskLevel());
		}

		double currentDwellTime = calculateAverageRiskDwellTime(riskHistoryByCompany, latestActualQuarterKey);
		int previousQuarterKey = QuarterCalculator.offset(QuarterCalculator.parseQuarterKey(latestActualQuarterKey), -1)
			.toQuarterKey();
		KpiCardDto.KpiDeltaDto delta = null;

		// 직전 분기 데이터가 존재할 때만 비교값을 내려준다.
		if (hasQuarterData(riskHistoryByCompany, previousQuarterKey)) {
			double previousDwellTime = calculateAverageRiskDwellTime(riskHistoryByCompany, previousQuarterKey);
			double difference = roundOneDecimal(currentDwellTime - previousDwellTime);
			KpiCardDto.KpiDeltaDto.Direction direction = KpiCardDto.KpiDeltaDto.Direction.FLAT;
			if (difference > 0.0) {
				direction = KpiCardDto.KpiDeltaDto.Direction.UP;
			} else if (difference < 0.0) {
				direction = KpiCardDto.KpiDeltaDto.Direction.DOWN;
			}
			delta = new KpiCardDto.KpiDeltaDto(
				Math.abs(difference),
				"분기",
				direction,
				"지난 분기 대비"
			);
		}

		KpiCardDto.KpiTone tone = currentDwellTime >= 2.0 ? KpiCardDto.KpiTone.WARN : KpiCardDto.KpiTone.DEFAULT;
		return new KpiCardDto(
			"RISK_DWELL_TIME",
			"리스크 체류 기간",
			currentDwellTime,
			"분기",
			tone,
			delta,
			null,
			new KpiCardDto.KpiTooltipDto(
				"주의/위험 상태에 머무른 평균 기간(분기 수)",
				"낮을수록 리스크 구간에서 빠르게 회복합니다.",
				"체류 기간이 긴 기업을 우선 점검하세요."
			)
		);
	}

	private boolean hasQuarterData(Map<Long, Map<Integer, CompanyKeyMetricRiskLevel>> riskHistoryByCompany, int quarterKey) {
		return riskHistoryByCompany.values().stream().anyMatch(history -> history.containsKey(quarterKey));
	}

	private double calculateAverageRiskDwellTime(
		Map<Long, Map<Integer, CompanyKeyMetricRiskLevel>> riskHistoryByCompany,
		int baseQuarterKey
	) {
		List<Integer> dwellTimes = new ArrayList<>();
		for (Map<Integer, CompanyKeyMetricRiskLevel> history : riskHistoryByCompany.values()) {
			CompanyKeyMetricRiskLevel baseLevel = history.get(baseQuarterKey);
			if (!isRisk(baseLevel)) {
				continue;
			}

			int dwell = 0;
			int cursorQuarterKey = baseQuarterKey;
			while (true) {
				CompanyKeyMetricRiskLevel level = history.get(cursorQuarterKey);
				if (!isRisk(level)) {
					break;
				}
				dwell++;
				cursorQuarterKey = QuarterCalculator.offset(QuarterCalculator.parseQuarterKey(cursorQuarterKey), -1).toQuarterKey();
			}
			dwellTimes.add(dwell);
		}

		if (dwellTimes.isEmpty()) {
			return 0.0;
		}
		double average = dwellTimes.stream().mapToInt(Integer::intValue).average().orElse(0.0);
		return roundOneDecimal(average);
	}

	private boolean isRisk(CompanyKeyMetricRiskLevel level) {
		return level == CompanyKeyMetricRiskLevel.WARN || level == CompanyKeyMetricRiskLevel.RISK;
	}

	private CompanyQuarterRiskDto.RiskLevel toRiskLevel(CompanyKeyMetricRiskLevel level) {
		return switch (level) {
			case SAFE -> CompanyQuarterRiskDto.RiskLevel.MIN;
			case WARN -> CompanyQuarterRiskDto.RiskLevel.WARN;
			case RISK -> CompanyQuarterRiskDto.RiskLevel.RISK;
		};
	}

	private double roundOneDecimal(double value) {
		return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
	}

	private List<KpiCardDto> buildKpis(
		int activeCompanies,
		RiskStatusDistributionDto distribution,
		double networkStatus,
		double riskIndex,
		KpiCardDto riskDwellTimeKpi
	) {
		int riskCompanies = distribution.RISK();
		double cautionRate = activeCompanies == 0
			? 0.0
			: BigDecimal.valueOf(distribution.CAUTION() * 100.0 / activeCompanies)
			.setScale(1, RoundingMode.HALF_UP)
			.doubleValue();

		KpiCardDto.KpiTone riskTone = riskCompanies > 0 ? KpiCardDto.KpiTone.RISK : KpiCardDto.KpiTone.GOOD;
		KpiCardDto.KpiTone cautionTone = cautionRate >= 20.0 ? KpiCardDto.KpiTone.WARN : KpiCardDto.KpiTone.DEFAULT;

		return List.of(
			new KpiCardDto(
				"ACTIVE_COMPANIES",
				"활성 관심기업",
				activeCompanies,
				null,
				KpiCardDto.KpiTone.DEFAULT,
				null,
				null,
				new KpiCardDto.KpiTooltipDto("로그인 사용자의 현재 워치리스트 기업 수", null, null)
			),
			new KpiCardDto(
				"RISK_COMPANIES",
				"고위험 기업 수",
				riskCompanies,
				null,
				riskTone,
				null,
				null,
				new KpiCardDto.KpiTooltipDto("최신 ACTUAL 분기에서 위험(RISK)으로 분류된 기업 수", null, null)
			),
			new KpiCardDto(
				"CAUTION_RATE",
				"주의 비율",
				cautionRate,
				"%",
				cautionTone,
				null,
				null,
				new KpiCardDto.KpiTooltipDto("최신 ACTUAL 분기에서 주의(CAUTION) 상태의 비율", null, null)
			),
			new KpiCardDto(
				"NETWORK_STATUS",
				"네트워크 상태",
				networkStatus,
				"%",
				KpiCardDto.KpiTone.DEFAULT,
				null,
				null,
				new KpiCardDto.KpiTooltipDto("활성 관심기업의 최신 ACTUAL 분기 internal_health_score 평균", null, null)
			),
			new KpiCardDto(
				"RISK_INDEX",
				"위험 지수",
				riskIndex,
				"점",
				riskIndex >= 60.0 ? KpiCardDto.KpiTone.RISK : (riskIndex >= 30.0 ? KpiCardDto.KpiTone.WARN : KpiCardDto.KpiTone.GOOD),
				null,
				null,
				new KpiCardDto.KpiTooltipDto(
					"포트폴리오 전체 위험 수준 요약 지표",
					"주의·위험 구간 증가 시 원인 분석이 필요합니다.",
					"위험 상위 협력사부터 상세 지표를 확인하세요."
				)
			),
			riskDwellTimeKpi
		);
	}

	private static class SectorAggregate {
		private int totalCount;
		private int warnCount;
		private int riskCount;
	}

	private String toQuarterLabel(int quarterKey) {
		YearQuarter yearQuarter = QuarterCalculator.parseQuarterKey(quarterKey);
		return yearQuarter.year() + "Q" + yearQuarter.quarter();
	}
}
