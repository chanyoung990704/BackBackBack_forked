package com.aivle.project.qna.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Q&A 게시글 생성 요청")
public record QaPostCreateRequest(
	@NotBlank(message = "제목은 필수입니다.")
	@Schema(description = "제목", example = "질문 제목")
	String title,
	
	@NotBlank(message = "내용은 필수입니다.")
	@Schema(description = "내용", example = "질문 내용")
	String body,
	
	@Schema(description = "작성자 (프론트엔드 전달용)", example = "홍길동")
	String author
) {
}
