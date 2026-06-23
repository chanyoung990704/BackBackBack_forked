package com.aivle.project.post.comment.dto;

import com.aivle.project.post.comment.entity.CommentsEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 댓글 응답 DTO.
 */
@Schema(description = "댓글 응답")
public record CommentResponse(
	@Schema(description = "댓글 ID", example = "10")
	Long id,
	@Schema(description = "작성자 성명", example = "홍길동")
	String name,
	@Schema(description = "게시글 ID", example = "100")
	Long postId,
	@Schema(description = "부모 댓글 ID", example = "5")
	Long parentId,
	@Schema(description = "댓글 내용", example = "좋은 글 감사합니다.")
	String content,
	@Schema(description = "댓글 깊이", example = "0")
	int depth,
	@Schema(description = "댓글 정렬 순서", example = "1")
	int sequence,
	@Schema(description = "생성 일시", example = "2026-01-25T12:34:56")
	LocalDateTime createdAt,
	@Schema(description = "수정 일시", example = "2026-01-25T12:40:00")
	LocalDateTime updatedAt
) {
}
