package com.aivle.project.post.service;

import com.aivle.project.common.error.CommonErrorCode;
import com.aivle.project.common.error.CommonException;
import com.aivle.project.post.entity.PostsEntity;
import com.aivle.project.user.entity.UserEntity;
import org.springframework.stereotype.Component;

/**
 * 게시글 읽기 접근 정책.
 */
@Component
public class PostReadAccessPolicy {

	private static final String BOARD_QNA = "qna";

	public void validateReadable(PostsEntity post, UserEntity user) {
		if (post == null || post.isDeleted()) {
			throw new CommonException(CommonErrorCode.COMMON_404);
		}

		if (!isQna(post)) {
			return;
		}

		if (user == null || user.getId() == null) {
			throw new CommonException(CommonErrorCode.COMMON_403);
		}

		if (!post.getUser().getId().equals(user.getId())) {
			throw new CommonException(CommonErrorCode.COMMON_403);
		}
	}

	private boolean isQna(PostsEntity post) {
		if (post.getCategory() == null || post.getCategory().getName() == null) {
			return false;
		}
		return BOARD_QNA.equalsIgnoreCase(post.getCategory().getName());
	}
}
