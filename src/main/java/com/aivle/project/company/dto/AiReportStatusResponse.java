package com.aivle.project.company.dto;

import java.time.LocalDateTime;

/**
 * AI 리포트 생성 상태 응답 DTO.
 */
public record AiReportStatusResponse(
	String requestId,
	String status,
	String message,
	String fileId,
	String downloadUrl,
	LocalDateTime createdAt,
	LocalDateTime completedAt
) {
	public AiReportStatusResponse(String requestId, String status) {
		this(requestId, status, null, null, null, null, null);
	}

	public static AiReportStatusResponse pending(String requestId) {
		return new AiReportStatusResponse(requestId, "PENDING", "리포트 생성 중입니다...", null, null, null, null);
	}

	public static AiReportStatusResponse processing(String requestId) {
		return new AiReportStatusResponse(requestId, "PROCESSING", "AI 서버에서 리포트 생성 중...", null, null, null, null);
	}

	public static AiReportStatusResponse completed(String requestId, String fileId, String downloadUrl) {
		return new AiReportStatusResponse(requestId, "COMPLETED", "리포트 생성이 완료되었습니다.", fileId, downloadUrl, null, LocalDateTime.now());
	}

	public static AiReportStatusResponse failed(String requestId, String message) {
		return new AiReportStatusResponse(requestId, "FAILED", message, null, null, null, null);
	}
}