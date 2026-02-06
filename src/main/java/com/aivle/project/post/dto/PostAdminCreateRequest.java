package com.aivle.project.post.dto;

import com.aivle.project.post.entity.PostStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 관리자용 게시글 생성 요청 DTO.
 */
@Schema(description = "관리자용 게시글 생성 요청")
@Getter
@Setter
public class PostAdminCreateRequest {

	@NotBlank
	@Size(max = 200)
	@Schema(description = "제목", example = "공지사항입니다.")
	private String title;

	@NotBlank
	@Schema(description = "내용", example = "시스템 점검 안내입니다.")
	private String content;

	@Schema(description = "상단 고정 여부", example = "true")
	private boolean isPinned = false;

	@NotNull
	@Schema(description = "게시 상태 (PUBLISHED, DRAFT, HIDDEN)", example = "PUBLISHED")
	private PostStatus status = PostStatus.PUBLISHED;
}
