package com.aivle.project.company.report.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 최신 예측 보고서 조회 응답.
 */
public record ReportLatestPredictResponse(
	String corpName,
	String stockCode,
	int quarterKey,
	Integer versionNo,
	LocalDateTime generatedAt,
	Long pdfFileId,
	String pdfFileName,
	String pdfContentType,
	String pdfDownloadUrl,
	List<ReportPredictMetricItemDto> metrics
) {
	public static ReportLatestPredictResponse empty(String stockCode, int quarterKey) {
		return new ReportLatestPredictResponse(
			null,
			stockCode,
			quarterKey,
			null,
			null,
			null,
			null,
			null,
			null,
			List.of()
		);
	}
}
