package com.aivle.project.report.dto;

/**
 * 보고서 지표 적재 결과.
 */
public record ReportPublishResult(
	int totalMetrics,
	int savedValues,
	int skippedMetrics,
	int skippedCompanies,
	Integer reportVersionNo
) {
	public static ReportPublishResult empty() {
		return new ReportPublishResult(0, 0, 0, 0, null);
	}
}
