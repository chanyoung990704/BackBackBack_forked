package com.aivle.project.report.dto;

/**
 * 보고서 PDF 업로드 결과.
 */
public record ReportPdfPublishResult(
	int saved,
	int skippedCompanies,
	Integer reportVersionNo,
	Long pdfFileId
) {
	public static ReportPdfPublishResult empty() {
		return new ReportPdfPublishResult(0, 0, null, null);
	}
}
