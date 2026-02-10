package com.aivle.project.post.controller;

import com.aivle.project.common.dto.ApiResponse;
import com.aivle.project.common.dto.PageRequest;
import com.aivle.project.common.dto.PageResponse;
import com.aivle.project.common.security.CurrentUser;
import com.aivle.project.post.dto.PostDetailResponse;
import com.aivle.project.post.dto.PostResponse;
import com.aivle.project.post.dto.PostUserCreateWithCategoryRequest;
import com.aivle.project.post.dto.PostUserCreateRequest;
import com.aivle.project.post.dto.PostUserUpdateRequest;
import com.aivle.project.post.service.PostService;
import com.aivle.project.user.entity.UserEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자용 게시판(보드) CRUD API.
 */
@Tag(name = "게시글 (사용자)", description = "사용자용 보드 기반 게시글 CRUD API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class PostController {

	private final PostService postService;

	@GetMapping(params = "categoryName")
	@Operation(summary = "게시글 목록 조회", description = "카테고리로 게시글 목록을 조회합니다.", security = {})
	public ResponseEntity<ApiResponse<PageResponse<PostResponse>>> listByCategory(
		@Parameter(description = "카테고리명 (notices, qna 등)", example = "qna")
		@RequestParam String categoryName,
		@RequestParam(defaultValue = "1") int page,
		@RequestParam(defaultValue = "10") int size,
		@RequestParam(defaultValue = "createdAt") String sortBy,
		@RequestParam(defaultValue = "DESC") String direction,
		@CurrentUser UserEntity user
	) {
		return ResponseEntity.ok(ApiResponse.ok(postService.list(categoryName, buildPageRequest(page, size, sortBy, direction), user)));
	}

	@GetMapping("/{categoryName}")
	@Operation(summary = "보드별 게시글 목록 조회(하위호환)", description = "특정 카테고리 보드의 게시글 목록을 조회합니다.", security = {})
	public ResponseEntity<ApiResponse<PageResponse<PostResponse>>> listLegacy(
		@Parameter(description = "카테고리명 (notices, qna 등)", example = "qna")
		@PathVariable String categoryName,
		@RequestParam(defaultValue = "1") int page,
		@RequestParam(defaultValue = "10") int size,
		@RequestParam(defaultValue = "createdAt") String sortBy,
		@RequestParam(defaultValue = "DESC") String direction,
		@CurrentUser UserEntity user
	) {
		return ResponseEntity.ok(ApiResponse.ok(postService.list(categoryName, buildPageRequest(page, size, sortBy, direction), user)));
	}

	@GetMapping("/{postId:\\d+}")
	@Operation(summary = "게시글 상세 조회", description = "게시글 ID로 상세 정보를 조회합니다.", security = {})
	public ResponseEntity<ApiResponse<PostDetailResponse>> getById(
		@PathVariable Long postId,
		@CurrentUser UserEntity user
	) {
		return ResponseEntity.ok(ApiResponse.ok(postService.getById(postId, user)));
	}

	@GetMapping("/{categoryName}/{postId}")
	@Operation(summary = "게시글 상세 조회(하위호환)", description = "보드 내 특정 게시글의 상세 정보를 조회합니다.", security = {})
	public ResponseEntity<ApiResponse<PostDetailResponse>> getLegacy(
		@PathVariable String categoryName,
		@PathVariable Long postId,
		@CurrentUser UserEntity user
	) {
		return ResponseEntity.ok(ApiResponse.ok(postService.get(categoryName, postId, user)));
	}

	@PostMapping
	@Operation(summary = "게시글 생성", description = "카테고리를 포함해 새 게시글을 생성합니다.")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<ApiResponse<PostResponse>> create(
		@CurrentUser UserEntity user,
		@Valid @RequestBody PostUserCreateWithCategoryRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.ok(postService.create(request.categoryName(), user, request.toLegacyRequest())));
	}

	@PostMapping("/{categoryName}")
	@Operation(summary = "게시글 생성(하위호환)", description = "특정 보드에 새 게시글을 생성합니다.")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<ApiResponse<PostResponse>> createLegacy(
		@PathVariable String categoryName,
		@CurrentUser UserEntity user,
		@Valid @RequestBody PostUserCreateRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.ok(postService.create(categoryName, user, request)));
	}

	@PatchMapping("/{postId:\\d+}")
	@Operation(summary = "게시글 수정", description = "게시글 ID 기준으로 본인이 작성한 게시글을 수정합니다.")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<ApiResponse<PostResponse>> updateById(
		@CurrentUser UserEntity user,
		@PathVariable Long postId,
		@Valid @RequestBody PostUserUpdateRequest request
	) {
		return ResponseEntity.ok(ApiResponse.ok(postService.updateById(user, postId, request)));
	}

	@PatchMapping("/{categoryName}/{postId}")
	@Operation(summary = "게시글 수정(하위호환)", description = "본인이 작성한 게시글을 수정합니다.")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<ApiResponse<PostResponse>> updateLegacy(
		@PathVariable String categoryName,
		@CurrentUser UserEntity user,
		@PathVariable Long postId,
		@Valid @RequestBody PostUserUpdateRequest request
	) {
		return ResponseEntity.ok(ApiResponse.ok(postService.update(categoryName, user, postId, request)));
	}

	@DeleteMapping("/{postId:\\d+}")
	@Operation(summary = "게시글 삭제", description = "게시글 ID 기준으로 본인이 작성한 게시글을 삭제합니다.")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<ApiResponse<Void>> deleteById(
		@CurrentUser UserEntity user,
		@PathVariable Long postId
	) {
		postService.deleteById(user, postId);
		return ResponseEntity.ok(ApiResponse.ok());
	}

	@DeleteMapping("/{categoryName}/{postId}")
	@Operation(summary = "게시글 삭제(하위호환)", description = "본인이 작성한 게시글을 삭제합니다.")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<ApiResponse<Void>> deleteLegacy(
		@PathVariable String categoryName,
		@CurrentUser UserEntity user,
		@PathVariable Long postId
	) {
		postService.delete(categoryName, user, postId);
		return ResponseEntity.ok(ApiResponse.ok());
	}

	private PageRequest buildPageRequest(int page, int size, String sortBy, String direction) {
		PageRequest pageRequest = new PageRequest();
		pageRequest.setPage(page);
		pageRequest.setSize(size);
		pageRequest.setSortBy(sortBy);
		pageRequest.setDirection(Sort.Direction.valueOf(direction));
		return pageRequest;
	}
}
