package com.aivle.project.company.report.dto;

/**
 * 보고서 지표 적재 결과.
 */
public record ReportImportResult(
	int totalCommands,
	int savedValues,
	int skippedCompanies,
	int skippedMetrics
) {

	public static ReportImportResult empty() {
		return new ReportImportResult(0, 0, 0, 0);
	}
}
