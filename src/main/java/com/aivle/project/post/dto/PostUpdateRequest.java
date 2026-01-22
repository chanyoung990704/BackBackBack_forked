package com.aivle.project.post.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 게시글 수정 요청 DTO.
 */
@Getter
@Setter
public class PostUpdateRequest {

	@Positive
	private Long categoryId;

	@Size(max = 200)
	private String title;

	private String content;
}
