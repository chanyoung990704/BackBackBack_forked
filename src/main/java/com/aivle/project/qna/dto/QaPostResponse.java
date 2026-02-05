package com.aivle.project.qna.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Q&A 게시글 응답")
public record QaPostResponse(
	@Schema(description = "게시글 ID", example = "1")
	String id,
	@Schema(description = "작성자 ID", example = "1")
	Long userId,
	@Schema(description = "제목", example = "질문 제목")
	String title,
	@Schema(description = "내용", example = "질문 내용")
	String body,
	@Schema(description = "작성자 이름", example = "홍길동")
	String author,
	@Schema(description = "생성 일시", example = "2026-02-05T12:00:00")
	LocalDateTime createdAt,
	@Schema(description = "수정 일시", example = "2026-02-05T12:00:00")
	LocalDateTime updatedAt,
	@Schema(description = "상태", example = "pending")
	String status,
	@Schema(description = "태그 목록")
	List<String> tags,
	@Schema(description = "답변 목록")
	List<QaReplyResponse> replies
) {
}
