package com.aivle.project.company.job;

import java.time.OffsetDateTime;

/**
 * Kafka로 전달되는 AI 작업 메시지.
 */
public record AiJobMessage(
	String requestId,
	AiJobType type,
	Long companyId,
	Integer year,
	Integer quarter,
	String period,
	OffsetDateTime requestedAt
) {
	public static AiJobMessage forReport(String requestId, Long companyId, Integer year, Integer quarter) {
		return new AiJobMessage(
			requestId,
			AiJobType.AI_REPORT,
			companyId,
			year,
			quarter,
			null,
			OffsetDateTime.now()
		);
	}

	public static AiJobMessage forCommentWarmup(String requestId, Long companyId, String period) {
		return new AiJobMessage(
			requestId,
			AiJobType.AI_COMMENT_WARMUP,
			companyId,
			null,
			null,
			period,
			OffsetDateTime.now()
		);
	}
}
