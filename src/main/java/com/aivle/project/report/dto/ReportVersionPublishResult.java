package com.aivle.project.report.dto;

/**
 * 보고서 버전 발행 결과.
 */
public record ReportVersionPublishResult(
	int published,
	int skippedCompanies,
	int skippedReports,
	int skippedVersions,
	Integer reportVersionNo
) {
	public static ReportVersionPublishResult empty() {
		return new ReportVersionPublishResult(0, 0, 0, 0, null);
	}
}
