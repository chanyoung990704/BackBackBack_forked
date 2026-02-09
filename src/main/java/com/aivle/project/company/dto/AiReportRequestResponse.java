package com.aivle.project.company.dto;

/**
 * AI 리포트 생성 요청 응답 DTO.
 */
public record AiReportRequestResponse(
	String requestId,
	String status,
	String message
) {
	public AiReportRequestResponse(String requestId) {
		this(requestId, "PENDING", "리포트 생성 요청이 접수되었습니다.");
	}
}