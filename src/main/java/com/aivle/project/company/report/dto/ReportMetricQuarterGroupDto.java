package com.aivle.project.company.report.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 분기별 지표 그룹 DTO.
 */
public record ReportMetricQuarterGroupDto(
	int quarterKey,
	int versionNo,
	LocalDateTime generatedAt,
	List<ReportMetricItemDto> metrics
) {
}
