package com.aivle.project.post.service;

import com.aivle.project.category.entity.CategoriesEntity;
import com.aivle.project.category.repository.CategoriesRepository;
import com.aivle.project.common.dto.PageRequest;
import com.aivle.project.common.dto.PageResponse;
import com.aivle.project.common.error.CommonErrorCode;
import com.aivle.project.common.error.CommonException;
import com.aivle.project.post.dto.PostCreateRequest;
import com.aivle.project.post.dto.PostResponse;
import com.aivle.project.post.dto.PostUpdateRequest;
import com.aivle.project.post.entity.PostStatus;
import com.aivle.project.post.entity.PostsEntity;
import com.aivle.project.post.repository.PostsRepository;
import com.aivle.project.user.entity.UserEntity;
import com.aivle.project.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 게시글 CRUD 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PostService {

	private final PostsRepository postsRepository;
	private final UserRepository userRepository;
	private final CategoriesRepository categoriesRepository;

	@Transactional(readOnly = true)
	public PageResponse<PostResponse> list(PageRequest pageRequest) {
		// 페이지 조건에 맞춰 게시글 목록을 최신순으로 조회합니다.
		Page<PostsEntity> page = postsRepository.findAllByDeletedAtIsNullOrderByCreatedAtDesc(
			pageRequest.toPageable()
		);
		return PageResponse.of(page.map(PostResponse::from));
	}

	@Transactional(readOnly = true)
	public PostResponse get(Long postId) {
		// 특정 게시글의 상세 정보를 조회합니다.
		PostsEntity post = findPost(postId);
		return PostResponse.from(post);
	}

	public PostResponse create(UUID userUuid, PostCreateRequest request) {
		// 사용자 및 카테고리 유효성을 확인한 후 새 게시글을 작성합니다.
		UserEntity user = findUser(userUuid);
		Long userId = user.getId();
		CategoriesEntity category = findCategory(request.getCategoryId());

		PostsEntity post = PostsEntity.create(
			user,
			category,
			request.getTitle().trim(),
			request.getContent().trim(),
			false,
			PostStatus.PUBLISHED,
			userId
		);

		PostsEntity saved = postsRepository.save(post);
		return PostResponse.from(saved);
	}

	public PostResponse update(UUID userUuid, Long postId, PostUpdateRequest request) {
		// 작성자 본인 확인 후 요청된 필드를 수정합니다.
		Long userId = findUser(userUuid).getId();
		PostsEntity post = findPost(postId);
		validateOwner(post, userId);

		String nextTitle = request.getTitle() != null ? request.getTitle().trim() : post.getTitle();
		String nextContent = request.getContent() != null ? request.getContent().trim() : post.getContent();
		CategoriesEntity nextCategory = post.getCategory();

		if (request.getCategoryId() != null) {
			nextCategory = findCategory(request.getCategoryId());
		}

		validatePatch(nextTitle, nextContent, request);

		post.update(nextTitle, nextContent, nextCategory, userId);
		return PostResponse.from(post);
	}

	public void delete(UUID userUuid, Long postId) {
		// 작성자 본인 확인 후 게시글을 삭제(소프트 삭제) 처리합니다.
		Long userId = findUser(userUuid).getId();
		PostsEntity post = findPost(postId);
		validateOwner(post, userId);
		post.markDeleted(userId);
	}

	private PostsEntity findPost(Long postId) {
		return postsRepository.findByIdAndDeletedAtIsNull(postId)
			.orElseThrow(() -> new CommonException(CommonErrorCode.COMMON_404));
	}

	private UserEntity findUser(UUID userUuid) {
		return userRepository.findByUuidAndDeletedAtIsNull(userUuid)
			.orElseThrow(() -> new CommonException(CommonErrorCode.COMMON_404));
	}

	private CategoriesEntity findCategory(Long categoryId) {
		return categoriesRepository.findByIdAndDeletedAtIsNull(categoryId)
			.orElseThrow(() -> new CommonException(CommonErrorCode.COMMON_404));
	}

	private void validateOwner(PostsEntity post, Long userId) {
		if (!post.getUser().getId().equals(userId)) {
			throw new CommonException(CommonErrorCode.COMMON_403);
		}
	}

	private void validatePatch(String title, String content, PostUpdateRequest request) {
		boolean hasAnyChange = request.getTitle() != null || request.getContent() != null || request.getCategoryId() != null;
		if (!hasAnyChange) {
			throw new CommonException(CommonErrorCode.COMMON_400);
		}
		if (title != null && title.isBlank()) {
			throw new CommonException(CommonErrorCode.COMMON_400_VALIDATION);
		}
		if (content != null && content.isBlank()) {
			throw new CommonException(CommonErrorCode.COMMON_400_VALIDATION);
		}
	}
}
