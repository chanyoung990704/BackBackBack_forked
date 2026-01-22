package com.aivle.project.post.dto;

import com.aivle.project.post.entity.PostsEntity;
import com.aivle.project.post.entity.PostStatus;
import java.time.LocalDateTime;

/**
 * 게시글 응답 DTO.
 */
public record PostResponse(
	Long id,
	Long userId,
	Long categoryId,
	String title,
	String content,
	int viewCount,
	boolean isPinned,
	PostStatus status,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
	public static PostResponse from(PostsEntity post) {
		return new PostResponse(
			post.getId(),
			post.getUser().getId(),
			post.getCategory().getId(),
			post.getTitle(),
			post.getContent(),
			post.getViewCount(),
			post.isPinned(),
			post.getStatus(),
			post.getCreatedAt(),
			post.getUpdatedAt()
		);
	}
}
