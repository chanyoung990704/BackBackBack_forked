package com.aivle.project.post.dto;

import com.aivle.project.post.entity.PostStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 게시글 상세 응답 DTO.
 */
@Schema(description = "게시글 상세 응답")
public record PostDetailResponse(
	@Schema(description = "게시글 ID", example = "100")
	Long id,
	@Schema(description = "작성자 성명", example = "홍길동")
	String name,
	@Schema(description = "카테고리 ID", example = "3")
	Long categoryId,
	@Schema(description = "제목", example = "첫 번째 게시글")
	String title,
	@Schema(description = "내용", example = "게시글 내용입니다.")
	String content,
	@Schema(description = "조회수", example = "123")
	int viewCount,
	@Schema(description = "고정 여부", example = "false")
	boolean isPinned,
	@Schema(description = "상태", example = "ACTIVE")
	PostStatus status,
	@Schema(description = "QnA 상태 (pending, answered)", example = "pending")
	String qnaStatus,
	@Schema(description = "생성 일시", example = "2026-01-25T12:34:56")
	LocalDateTime createdAt,
	@Schema(description = "수정 일시", example = "2026-01-25T12:40:00")
	LocalDateTime updatedAt,
	@Schema(description = "첨부 파일 목록")
	List<PostFileResponse> files
) {
}
