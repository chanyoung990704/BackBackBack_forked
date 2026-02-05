package com.aivle.project.qna.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Q&A 답변 응답")
public record QaReplyResponse(
	@Schema(description = "답변 ID", example = "1")
	String id,
	@Schema(description = "작성자 이름", example = "Decision Desk")
	String author,
	@Schema(description = "생성 일시", example = "2026-02-05T12:00:00")
	LocalDateTime createdAt,
	@Schema(description = "내용", example = "답변 내용입니다.")
	String body
) {
}
