package com.aivle.project.company.service;

import com.aivle.project.company.dto.CompanyInfoDto;
import com.aivle.project.company.dto.CompanyOverviewDataPointDto;
import com.aivle.project.company.dto.CompanyOverviewDataType;
import com.aivle.project.company.dto.CompanyOverviewForecastDto;
import com.aivle.project.company.dto.CompanyOverviewKeyMetricDto;
import com.aivle.project.company.dto.CompanyOverviewMetricDto;
import com.aivle.project.company.dto.CompanyOverviewMetricSeriesDto;
import com.aivle.project.company.dto.CompanyOverviewResponseDto;
import com.aivle.project.company.dto.CompanyOverviewSignalLevel;
import com.aivle.project.company.dto.CompanyOverviewTooltipDto;
import com.aivle.project.company.keymetric.entity.CompanyKeyMetricEntity;
import com.aivle.project.company.keymetric.repository.CompanyKeyMetricRepository;
import com.aivle.project.company.keymetric.repository.KeyMetricDescriptionRepository;
import com.aivle.project.metric.entity.MetricValueType;
import com.aivle.project.quarter.entity.QuartersEntity;
import com.aivle.project.quarter.repository.QuartersRepository;
import com.aivle.project.quarter.support.QuarterCalculator;
import com.aivle.project.quarter.support.YearQuarter;
import com.aivle.project.report.dto.CompanyOverviewMetricRowProjection;
import com.aivle.project.report.entity.SignalColor;
import com.aivle.project.report.repository.CompanyReportMetricValuesRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 기업 개요 응답 조립 서비스.
 */
@Service
@RequiredArgsConstructor
public class CompanyOverviewService {

	private static final String LOCALE_KO = "ko";
	private static final List<String> KEY_METRIC_CODES = List.of(
		"NETWORK_HEALTH",
		"EXTERNAL_HEALTH",
		"EXTERNAL_REPUTATION"
	);

	private final CompanyInfoService companyInfoService;
	private final QuartersRepository quartersRepository;
	private final CompanyKeyMetricRepository companyKeyMetricRepository;
	private final KeyMetricDescriptionRepository keyMetricDescriptionRepository;
	private final CompanyReportMetricValuesRepository companyReportMetricValuesRepository;

	/**
	 * 기업 개요 응답을 구성한다.
	 */
	public CompanyOverviewResponseDto getOverview(Long companyId, String quarterKey) {
		CompanyInfoDto companyInfo = companyInfoService.getCompanyInfo(companyId, quarterKey);
		int parsedQuarterKey = parseQuarterKey(quarterKey);
		QuartersEntity quarter = quartersRepository.findByQuarterKey(parsedQuarterKey)
			.orElseThrow(() -> new IllegalArgumentException("Quarter not found for key: " + parsedQuarterKey));

		CompanyKeyMetricEntity keyMetric = companyKeyMetricRepository
			.findByCompanyIdAndQuarterId(companyId, quarter.getId())
			.orElse(null);

		List<CompanyOverviewMetricRowProjection> seriesRows = loadSeriesRows(companyInfo.getStockCode(), parsedQuarterKey);
		CompanyOverviewForecastDto forecast = buildForecast(parsedQuarterKey, seriesRows);
		List<CompanyOverviewKeyMetricDto> keyMetrics = buildKeyMetrics(keyMetric);
		List<CompanyOverviewMetricDto> metrics = buildMetrics(seriesRows, parsedQuarterKey);
		String aiComment = keyMetric != null ? keyMetric.getAiComment() : null;

		return new CompanyOverviewResponseDto(
			companyInfo,
			forecast,
			keyMetrics,
			metrics,
			aiComment
		);
	}

	private List<CompanyOverviewMetricRowProjection> loadSeriesRows(String stockCode, int quarterKey) {
		YearQuarter baseQuarter = QuarterCalculator.parseQuarterKey(quarterKey);
		int fromQuarterKey = QuarterCalculator.offset(baseQuarter, -3).toQuarterKey();
		int toQuarterKey = QuarterCalculator.offset(baseQuarter, 1).toQuarterKey();

		return companyReportMetricValuesRepository
			.findLatestOverviewMetricsByStockCodeAndQuarterRange(stockCode, fromQuarterKey, toQuarterKey, LOCALE_KO);
	}

	private CompanyOverviewForecastDto buildForecast(
		int quarterKey,
		List<CompanyOverviewMetricRowProjection> rows
	) {
		YearQuarter baseQuarter = QuarterCalculator.parseQuarterKey(quarterKey);
		YearQuarter nextQuarter = QuarterCalculator.offset(baseQuarter, 1);
		List<CompanyOverviewMetricSeriesDto> series = buildSeries(rows);

		return new CompanyOverviewForecastDto(
			formatQuarterKey(quarterKey),
			formatQuarterKey(nextQuarter.toQuarterKey()),
			series
		);
	}

	private List<CompanyOverviewMetricSeriesDto> buildSeries(List<CompanyOverviewMetricRowProjection> rows) {
		Map<String, CompanyOverviewMetricSeriesDto> seriesMap = new LinkedHashMap<>();
		for (CompanyOverviewMetricRowProjection row : rows) {
			String metricCode = row.getMetricCode();
			CompanyOverviewMetricSeriesDto series = seriesMap.computeIfAbsent(metricCode, key -> new CompanyOverviewMetricSeriesDto(
				metricCode,
				row.getMetricNameKo(),
				row.getUnit(),
				new ArrayList<>()
			));
			CompanyOverviewDataPointDto dataPoint = new CompanyOverviewDataPointDto(
				formatQuarterKey(row.getQuarterKey()),
				toDouble(row.getMetricValue()),
				mapDataType(row.getValueType())
			);
			series.getPoints().add(dataPoint);
		}
		return new ArrayList<>(seriesMap.values());
	}

	private List<CompanyOverviewKeyMetricDto> buildKeyMetrics(CompanyKeyMetricEntity keyMetric) {
		List<CompanyOverviewKeyMetricDto> result = new ArrayList<>();
		Map<String, BigDecimal> valueMap = new LinkedHashMap<>();
		if (keyMetric != null) {
			valueMap.put("NETWORK_HEALTH", keyMetric.getInternalHealthScore());
			valueMap.put("EXTERNAL_HEALTH", keyMetric.getExternalHealthScore());
			valueMap.put("EXTERNAL_REPUTATION", null);
		}

		for (String metricCode : KEY_METRIC_CODES) {
			var description = keyMetricDescriptionRepository.findByMetricCode(metricCode).orElse(null);
			CompanyOverviewTooltipDto tooltip = description == null ? null : new CompanyOverviewTooltipDto(
				description.getDescription(),
				description.getInterpretation(),
				description.getActionHint()
			);
			result.add(new CompanyOverviewKeyMetricDto(
				metricCode,
				description != null ? description.getMetricName() : metricCode,
				toDouble(valueMap.get(metricCode)),
				description != null ? description.getUnit() : null,
				tooltip
			));
		}
		return result;
	}

	private List<CompanyOverviewMetricDto> buildMetrics(
		List<CompanyOverviewMetricRowProjection> rows,
		int quarterKey
	) {
		YearQuarter baseQuarter = QuarterCalculator.parseQuarterKey(quarterKey);
		int nextQuarterKey = QuarterCalculator.offset(baseQuarter, 1).toQuarterKey();
		List<CompanyOverviewMetricDto> result = new ArrayList<>();
		for (CompanyOverviewMetricRowProjection row : rows) {
			if (row.getQuarterKey() != nextQuarterKey || row.getValueType() != MetricValueType.PREDICTED) {
				continue;
			}
			CompanyOverviewTooltipDto tooltip = buildMetricTooltip(row);
			result.add(new CompanyOverviewMetricDto(
				row.getMetricCode(),
				row.getMetricNameKo(),
				mapSignalLevel(row.getSignalColor()),
				toDouble(row.getMetricValue()),
				row.getUnit(),
				tooltip
			));
		}
		return result;
	}

	private CompanyOverviewTooltipDto buildMetricTooltip(CompanyOverviewMetricRowProjection row) {
		if (row.getDescription() == null && row.getInterpretation() == null && row.getActionHint() == null) {
			return null;
		}
		return new CompanyOverviewTooltipDto(
			row.getDescription(),
			row.getInterpretation(),
			row.getActionHint()
		);
	}

	private int parseQuarterKey(String quarterKey) {
		try {
			String normalized = normalizeQuarterKey(quarterKey);
			int parsed = Integer.parseInt(normalized);
			YearQuarter yearQuarter = QuarterCalculator.parseQuarterKey(parsed);
			return yearQuarter.toQuarterKey();
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("quarterKey는 숫자 형식이어야 합니다.", e);
		}
	}

	private String normalizeQuarterKey(String quarterKey) {
		if (quarterKey == null) {
			return null;
		}
		String trimmed = quarterKey.trim();
		if (trimmed.length() == 6) {
			String yearPart = trimmed.substring(0, 4);
			String quarterPart = trimmed.substring(4, 6);
			if (quarterPart.startsWith("0")) {
				quarterPart = quarterPart.substring(1);
			}
			return yearPart + quarterPart;
		}
		return trimmed;
	}

	private String formatQuarterKey(int quarterKey) {
		YearQuarter yearQuarter = QuarterCalculator.parseQuarterKey(quarterKey);
		return yearQuarter.year() + "Q" + yearQuarter.quarter();
	}

	private CompanyOverviewSignalLevel mapSignalLevel(SignalColor signalColor) {
		if (signalColor == null) {
			return CompanyOverviewSignalLevel.UNKNOWN;
		}
		return switch (signalColor) {
			case GREEN -> CompanyOverviewSignalLevel.GREEN;
			case YELLOW -> CompanyOverviewSignalLevel.YELLOW;
			case RED -> CompanyOverviewSignalLevel.RED;
		};
	}

	private CompanyOverviewDataType mapDataType(MetricValueType valueType) {
		if (valueType == null) {
			return CompanyOverviewDataType.ACTUAL;
		}
		return valueType == MetricValueType.ACTUAL ? CompanyOverviewDataType.ACTUAL : CompanyOverviewDataType.PRED;
	}

	private Double toDouble(BigDecimal value) {
		return value == null ? null : value.doubleValue();
	}
}
