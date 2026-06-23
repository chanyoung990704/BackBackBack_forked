package com.aivle.project.post.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 댓글 수정 요청 DTO.
 */
@Schema(description = "댓글 수정 요청")
@Getter
@Setter
public class CommentUpdateRequest {

	@NotBlank
	@Schema(description = "댓글 내용", example = "내용을 수정했습니다.")
	private String content;
}
