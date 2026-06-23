package com.aivle.project.company.report.dto;

import java.util.List;

/**
 * 분기별 그룹핑 응답 DTO.
 */
public record ReportMetricGroupedResponse(
	String corpName,
	String stockCode,
	int fromQuarterKey,
	int toQuarterKey,
	List<ReportMetricQuarterGroupDto> quarters
) {

	public static ReportMetricGroupedResponse empty(String stockCode, int fromQuarterKey, int toQuarterKey) {
		return new ReportMetricGroupedResponse(null, stockCode, fromQuarterKey, toQuarterKey, List.of());
	}
}
