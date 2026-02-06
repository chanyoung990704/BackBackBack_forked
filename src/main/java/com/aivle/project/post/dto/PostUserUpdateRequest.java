package com.aivle.project.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 사용자 게시글 수정 요청 DTO.
 * 카테고리는 URL 경로에서 결정되므로 포함하지 않습니다.
 */
@Schema(description = "사용자 게시글 수정 요청")
@Getter
@Setter
public class PostUserUpdateRequest {

	@Size(max = 200)
	@Schema(description = "제목", example = "수정된 질문 제목")
	private String title;

	@Schema(description = "내용", example = "수정된 질문 내용입니다.")
	private String content;
}
