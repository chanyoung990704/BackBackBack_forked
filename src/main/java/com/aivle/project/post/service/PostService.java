package com.aivle.project.post.service;

import com.aivle.project.category.entity.CategoriesEntity;
import com.aivle.project.category.repository.CategoriesRepository;
import com.aivle.project.common.dto.PageRequest;
import com.aivle.project.common.dto.PageResponse;
import com.aivle.project.common.error.CommonErrorCode;
import com.aivle.project.common.error.CommonException;
import com.aivle.project.file.repository.PostFilesRepository;
import com.aivle.project.file.mapper.FileMapper;
import com.aivle.project.post.dto.PostAdminCreateRequest;
import com.aivle.project.post.dto.PostAdminUpdateRequest;
import com.aivle.project.post.dto.PostDetailResponse;
import com.aivle.project.post.dto.PostFileResponse;
import com.aivle.project.post.dto.PostCreateRequest;
import com.aivle.project.post.dto.PostResponse;
import com.aivle.project.post.dto.PostUpdateRequest;
import com.aivle.project.post.dto.PostUserCreateRequest;
import com.aivle.project.post.dto.PostUserUpdateRequest;
import com.aivle.project.post.entity.PostStatus;
import com.aivle.project.post.entity.PostsEntity;
import com.aivle.project.post.entity.PostViewCountsEntity;
import com.aivle.project.post.repository.PostViewCountsRepository;
import com.aivle.project.post.repository.PostsRepository;
import com.aivle.project.user.entity.UserEntity;
import com.aivle.project.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 및 관리자 게시글 CRUD 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PostService {

	private final PostsRepository postsRepository;
	private final PostViewCountsRepository postViewCountsRepository;
	private final CategoriesRepository categoriesRepository;
	private final UserRepository userRepository;
	private final com.aivle.project.post.mapper.PostMapper postMapper;
	private final PostFilesRepository postFilesRepository;
	private final FileMapper fileMapper;

	private static final String BOARD_NOTICES = "notices";
	private static final String BOARD_QNA = "qna";

	/**
	 * [사용자] 보드별 게시글 목록 조회.
	 */
	@Transactional(readOnly = true)
	public PageResponse<PostResponse> list(String categoryName, PageRequest pageRequest, UserEntity user) {
		CategoriesEntity category = findCategoryByName(categoryName);
		
		Page<PostsEntity> page;
		if (BOARD_QNA.equalsIgnoreCase(categoryName)) {
			// QnA 보드는 본인 글만 조회 가능 (비로그인 접근 불가)
			if (user == null) {
				throw new CommonException(CommonErrorCode.COMMON_403);
			}
			page = postsRepository.findAllByCategoryNameAndUserIdAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
				categoryName, user.getId(), PostStatus.PUBLISHED, pageRequest.toPageable()
			);
		} else {
			page = postsRepository.findAllByCategoryNameAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
				categoryName, PostStatus.PUBLISHED, pageRequest.toPageable()
			);
		}
		
		return PageResponse.of(page.map(postMapper::toResponse));
	}

	/**
	 * [사용자] 게시글 상세 조회.
	 */
	@Transactional(readOnly = true)
	public PostDetailResponse get(String categoryName, Long postId, UserEntity user) {
		PostsEntity post = findPostInBoard(postId, categoryName);
		
		if (BOARD_QNA.equalsIgnoreCase(categoryName)) {
			if (user == null) {
				throw new CommonException(CommonErrorCode.COMMON_403);
			}
			validateOwner(post, user.getId());
		}

		PostResponse response = postMapper.toResponse(post);
		boolean downloadable = user != null && user.getId() != null;
		List<PostFileResponse> files = postFilesRepository.findAllActiveByPostIdOrderByCreatedAtAsc(postId).stream()
			.map(mapping -> fileMapper.toResponse(postId, mapping.getFile()))
			.map(file -> new PostFileResponse(
				file.id(),
				file.originalFilename(),
				file.fileSize(),
				file.contentType(),
				file.createdAt(),
				downloadable,
				downloadable ? buildDownloadUrl(file.id()) : null
			))
			.toList();

		return new PostDetailResponse(
			response.id(),
			response.name(),
			response.categoryId(),
			response.title(),
			response.content(),
			response.viewCount(),
			response.isPinned(),
			response.status(),
			response.qnaStatus(),
			response.createdAt(),
			response.updatedAt(),
			files
		);
	}

	/**
	 * [사용자] 게시글 상세 조회(카테고리 경로 없이 ID 기준).
	 */
	@Transactional(readOnly = true)
	public PostDetailResponse getById(Long postId, UserEntity user) {
		PostsEntity post = findPost(postId);
		String categoryName = post.getCategory().getName();
		if (BOARD_QNA.equalsIgnoreCase(categoryName)) {
			if (user == null) {
				throw new CommonException(CommonErrorCode.COMMON_403);
			}
			validateOwner(post, user.getId());
		}

		PostResponse response = postMapper.toResponse(post);
		boolean downloadable = user != null && user.getId() != null;
		List<PostFileResponse> files = postFilesRepository.findAllActiveByPostIdOrderByCreatedAtAsc(postId).stream()
			.map(mapping -> fileMapper.toResponse(postId, mapping.getFile()))
			.map(file -> new PostFileResponse(
				file.id(),
				file.originalFilename(),
				file.fileSize(),
				file.contentType(),
				file.createdAt(),
				downloadable,
				downloadable ? buildDownloadUrl(file.id()) : null
			))
			.toList();

		return new PostDetailResponse(
			response.id(),
			response.name(),
			response.categoryId(),
			response.title(),
			response.content(),
			response.viewCount(),
			response.isPinned(),
			response.status(),
			response.qnaStatus(),
			response.createdAt(),
			response.updatedAt(),
			files
		);
	}

	/**
	 * [사용자] 게시글 생성.
	 */
	public PostResponse create(String categoryName, UserEntity user, PostUserCreateRequest request) {
		validateUserWriteAccess(categoryName);
		CategoriesEntity category = findCategoryByName(categoryName);

		PostsEntity post = PostsEntity.create(
			user,
			category,
			request.getTitle().trim(),
			request.getContent().trim(),
			false,
			PostStatus.PUBLISHED
		);

		PostsEntity saved = postsRepository.save(post);
		postViewCountsRepository.save(PostViewCountsEntity.create(saved));
		return postMapper.toResponse(saved);
	}

	/**
	 * [사용자] 게시글 수정.
	 */
	public PostResponse update(String categoryName, UserEntity user, Long postId, PostUserUpdateRequest request) {
		PostsEntity post = findPostInBoard(postId, categoryName);
		validateOwner(post, requireUserId(user));

		String nextTitle = request.getTitle() != null ? request.getTitle().trim() : post.getTitle();
		String nextContent = request.getContent() != null ? request.getContent().trim() : post.getContent();

		post.update(nextTitle, nextContent, post.getCategory(), null, null);
		return postMapper.toResponse(post);
	}

	/**
	 * [사용자] 게시글 수정(카테고리 경로 없이 ID 기준).
	 */
	public PostResponse updateById(UserEntity user, Long postId, PostUserUpdateRequest request) {
		PostsEntity post = findPost(postId);
		validateOwner(post, requireUserId(user));

		String nextTitle = request.getTitle() != null ? request.getTitle().trim() : post.getTitle();
		String nextContent = request.getContent() != null ? request.getContent().trim() : post.getContent();

		post.update(nextTitle, nextContent, post.getCategory(), null, null);
		return postMapper.toResponse(post);
	}

	/**
	 * [사용자] 게시글 삭제.
	 */
	public void delete(String categoryName, UserEntity user, Long postId) {
		PostsEntity post = findPostInBoard(postId, categoryName);
		validateOwner(post, requireUserId(user));
		post.markDeleted();
	}

	/**
	 * [사용자] 게시글 삭제(카테고리 경로 없이 ID 기준).
	 */
	public void deleteById(UserEntity user, Long postId) {
		PostsEntity post = findPost(postId);
		validateOwner(post, requireUserId(user));
		post.markDeleted();
	}

	// ============================================
	// Admin Operations
	// ============================================

	/**
	 * [관리자] 보드별 전체 게시글 목록 조회.
	 */
	@Transactional(readOnly = true)
	public PageResponse<PostResponse> listAdmin(String categoryName, PageRequest pageRequest) {
		Page<PostsEntity> page = postsRepository.findAllByCategoryNameAndDeletedAtIsNullOrderByCreatedAtDesc(
			categoryName, pageRequest.toPageable()
		);
		return PageResponse.of(page.map(postMapper::toResponse));
	}

	/**
	 * [관리자] 게시글 상세 조회.
	 */
	@Transactional(readOnly = true)
	public PostResponse getAdmin(String categoryName, Long postId) {
		PostsEntity post = findPostInBoard(postId, categoryName);
		return postMapper.toResponse(post);
	}

	/**
	 * [관리자] 게시글 생성.
	 */
	public PostResponse createAdmin(String categoryName, UserEntity admin, PostAdminCreateRequest request) {
		CategoriesEntity category = findCategoryByName(categoryName);

		PostsEntity post = PostsEntity.create(
			admin,
			category,
			request.getTitle().trim(),
			request.getContent().trim(),
			request.isPinned(),
			request.getStatus()
		);

		PostsEntity saved = postsRepository.save(post);
		postViewCountsRepository.save(PostViewCountsEntity.create(saved));
		return postMapper.toResponse(saved);
	}

	/**
	 * [관리자] 게시글 수정.
	 */
	public PostResponse updateAdmin(String categoryName, Long postId, PostAdminUpdateRequest request) {
		PostsEntity post = findPostInBoard(postId, categoryName);

		String nextTitle = request.getTitle() != null ? request.getTitle().trim() : post.getTitle();
		String nextContent = request.getContent() != null ? request.getContent().trim() : post.getContent();

		post.update(nextTitle, nextContent, post.getCategory(), request.getIsPinned(), request.getStatus());
		return postMapper.toResponse(post);
	}

	/**
	 * [관리자] 게시글 삭제.
	 */
	public void deleteAdmin(String categoryName, Long postId) {
		PostsEntity post = findPostInBoard(postId, categoryName);
		post.markDeleted();
	}

	// ============================================
	// Helpers
	// ============================================

	private CategoriesEntity findCategoryByName(String name) {
		return categoriesRepository.findByNameAndDeletedAtIsNull(name)
			.orElseThrow(() -> new CommonException(CommonErrorCode.COMMON_404));
	}

	private PostsEntity findPostInBoard(Long postId, String categoryName) {
		return postsRepository.findByIdAndCategoryNameAndDeletedAtIsNull(postId, categoryName)
			.orElseThrow(() -> new CommonException(CommonErrorCode.COMMON_404));
	}

	private PostsEntity findPost(Long postId) {
		return postsRepository.findByIdAndDeletedAtIsNull(postId)
			.orElseThrow(() -> new CommonException(CommonErrorCode.COMMON_404));
	}

	private void validateUserWriteAccess(String categoryName) {
		if (BOARD_NOTICES.equalsIgnoreCase(categoryName)) {
			throw new CommonException(CommonErrorCode.COMMON_403);
		}
	}

	private void validateOwner(PostsEntity post, Long userId) {
		if (!post.getUser().getId().equals(userId)) {
			throw new CommonException(CommonErrorCode.COMMON_403);
		}
	}

	private String buildDownloadUrl(Long fileId) {
		return "/api/files/" + fileId;
	}

	private Long requireUserId(UserEntity user) {
		if (user == null || user.getId() == null) {
			throw new CommonException(CommonErrorCode.COMMON_403);
		}
		return user.getId();
	}

	private UserEntity getCurrentUserOrThrow() {
		org.springframework.security.core.Authentication auth = 
			org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !auth.isAuthenticated()) {
			throw new CommonException(CommonErrorCode.COMMON_403);
		}
		
		Object principal = auth.getPrincipal();
		if (!(principal instanceof org.springframework.security.oauth2.jwt.Jwt jwt)) {
			throw new CommonException(CommonErrorCode.COMMON_403);
		}

		java.util.UUID userUuid = java.util.UUID.fromString(jwt.getSubject());
		return userRepository.findByUuidAndDeletedAtIsNull(userUuid)
			.orElseThrow(() -> new CommonException(CommonErrorCode.COMMON_404));
	}
}
