package com.aivle.project.company.report.dto;

/**
 * 보고서 지표 예측값 적재 결과.
 */
public record ReportPredictResult(
	int totalMetrics,
	int savedValues,
	int skippedMetrics,
	int skippedCompanies,
	Integer reportVersionNo
) {
	public static ReportPredictResult empty() {
		return new ReportPredictResult(0, 0, 0, 0, null);
	}
}
