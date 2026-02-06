package com.aivle.project.post.dto;

import com.aivle.project.post.entity.PostStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 관리자용 게시글 수정 요청 DTO.
 */
@Schema(description = "관리자용 게시글 수정 요청")
@Getter
@Setter
public class PostAdminUpdateRequest {

	@Size(max = 200)
	@Schema(description = "제목", example = "수정된 공지사항")
	private String title;

	@Schema(description = "내용", example = "수정된 점검 안내 내용입니다.")
	private String content;

	@Schema(description = "상단 고정 여부", example = "false")
	private Boolean isPinned;

	@Schema(description = "게시 상태 (PUBLISHED, DRAFT, HIDDEN)", example = "HIDDEN")
	private PostStatus status;
}
