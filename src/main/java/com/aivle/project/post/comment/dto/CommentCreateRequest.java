package com.aivle.project.post.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 댓글 생성 요청 DTO.
 */
@Schema(description = "댓글 생성 요청")
@Getter
@Setter
public class CommentCreateRequest {

	@NotNull
	@Schema(description = "게시글 ID", example = "100")
	private Long postId;

	@Schema(description = "부모 댓글 ID(대댓글)", example = "10")
	private Long parentId;

	@NotBlank
	@Schema(description = "댓글 내용", example = "좋은 글 감사합니다.")
	private String content;
}
