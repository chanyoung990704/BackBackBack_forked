package com.aivle.project.company.job;

import java.util.Map;

/**
 * Python AI Worker로부터 수신할 AI 작업 결과 메시지.
 */
public record AiJobResponseMessage(
	String requestId,
	AiJobType type,
	String status, // "SUCCESS" | "FAILED"
	String errorMessage,
	Long companyId,
	Integer year,
	Integer quarter,
	Map<String, Double> predictions, // AI_FINANCIAL_ANALYSIS 결과 수치
	String storageKey,               // AI_COMMENT_COMPILATION 결과 PDF 스토리지 키
	String filename                  // 원본 파일명
) {
	public boolean isSuccess() {
		return "SUCCESS".equalsIgnoreCase(status);
	}
}
