package com.aivle.project.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 게시글 생성 요청 DTO.
 */
@Getter
@Setter
public class PostCreateRequest {

	@NotNull
	private Long categoryId;

	@NotBlank
	@Size(max = 200)
	private String title;

	@NotBlank
	private String content;
}
