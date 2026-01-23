package com.aivle.project.comment.service;

import com.aivle.project.comment.dto.CommentCreateRequest;
import com.aivle.project.comment.dto.CommentResponse;
import com.aivle.project.comment.dto.CommentUpdateRequest;
import com.aivle.project.comment.entity.CommentsEntity;
import com.aivle.project.comment.repository.CommentsRepository;
import com.aivle.project.common.error.CommonErrorCode;
import com.aivle.project.common.error.CommonException;
import com.aivle.project.post.entity.PostsEntity;
import com.aivle.project.post.repository.PostsRepository;
import com.aivle.project.user.entity.UserEntity;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 댓글 CRUD 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CommentsService {

	private final CommentsRepository commentsRepository;
	private final PostsRepository postsRepository;

	@Transactional(readOnly = true)
	public List<CommentResponse> listByPost(Long postId) {
		return commentsRepository.findByPostIdOrderByDepthAscSequenceAsc(postId).stream()
			.map(CommentResponse::from)
			.toList();
	}

	public CommentResponse create(UserEntity user, CommentCreateRequest request) {
		Long userId = requireUserId(user);
		PostsEntity post = findPost(request.getPostId());

		CommentsEntity parent = null;
		int depth = 0;
		int sequence;

		if (request.getParentId() != null) {
			parent = findComment(request.getParentId());
			if (!parent.getPost().getId().equals(post.getId())) {
				throw new CommonException(CommonErrorCode.COMMON_400); // 부모 댓글이 같은 게시글이 아님
			}
			depth = parent.getDepth() + 1;
			sequence = commentsRepository.findMaxSequenceByParentId(parent.getId()) + 1;
		} else {
			sequence = commentsRepository.findMaxSequenceByPostIdAndParentIsNull(post.getId()) + 1;
		}

		CommentsEntity comment = CommentsEntity.create(
			post,
			user,
			parent,
			request.getContent().trim(),
			depth,
			sequence,
			userId
		);

		CommentsEntity saved = commentsRepository.save(comment);
		return CommentResponse.from(saved);
	}

	public CommentResponse update(Long userId, Long commentId, CommentUpdateRequest request) {
		validateUserId(userId);
		CommentsEntity comment = findComment(commentId);
		validateOwner(comment, userId);

		comment.update(request.getContent().trim(), userId);
		return CommentResponse.from(comment);
	}

	public void delete(Long userId, Long commentId) {
		validateUserId(userId);
		CommentsEntity comment = findComment(commentId);
		validateOwner(comment, userId);
		comment.markDeleted(userId);
	}

	private CommentsEntity findComment(Long commentId) {
		return commentsRepository.findById(commentId)
			.filter(c -> !c.isDeleted())
			.orElseThrow(() -> new CommonException(CommonErrorCode.COMMON_404));
	}

	private PostsEntity findPost(Long postId) {
		return postsRepository.findByIdAndDeletedAtIsNull(postId)
			.orElseThrow(() -> new CommonException(CommonErrorCode.COMMON_404));
	}

	private void validateOwner(CommentsEntity comment, Long userId) {
		if (!comment.getUser().getId().equals(userId)) {
			throw new CommonException(CommonErrorCode.COMMON_403);
		}
	}

	private void validateUserId(Long userId) {
		if (userId == null || userId <= 0) {
			throw new CommonException(CommonErrorCode.COMMON_400);
		}
	}

	private Long requireUserId(UserEntity user) {
		if (user == null || user.getId() == null) {
			throw new CommonException(CommonErrorCode.COMMON_403);
		}
		return user.getId();
	}
}
