package com.aivle.project.qna.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Q&A 답변 생성 요청")
public record QaReplyCreateRequest(
	@NotBlank(message = "내용은 필수입니다.")
	@Schema(description = "내용", example = "답변 내용입니다.")
	String body,
	
	@Schema(description = "작성자 (프론트엔드 전달용)", example = "Decision Desk")
	String author
) {
}
