package com.aivle.project.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 사용자 게시글 생성 요청 DTO (RESTful 경로용).
 */
@Schema(description = "사용자 게시글 생성 요청(카테고리 포함)")
public record PostUserCreateWithCategoryRequest(
	@NotBlank
	@Schema(description = "카테고리명", example = "qna")
	String categoryName,

	@NotBlank
	@Size(max = 200)
	@Schema(description = "제목", example = "질문 있습니다.")
	String title,

	@NotBlank
	@Schema(description = "내용", example = "이 기능은 어떻게 사용하나요?")
	String content
) {
	public PostUserCreateRequest toLegacyRequest() {
		PostUserCreateRequest request = new PostUserCreateRequest();
		request.setTitle(title);
		request.setContent(content);
		return request;
	}
}
