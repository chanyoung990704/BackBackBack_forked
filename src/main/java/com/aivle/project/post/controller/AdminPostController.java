package com.aivle.project.post.controller;

import com.aivle.project.common.dto.ApiResponse;
import com.aivle.project.common.dto.PageRequest;
import com.aivle.project.common.dto.PageResponse;
import com.aivle.project.common.security.CurrentUser;
import com.aivle.project.post.dto.PostAdminCreateRequest;
import com.aivle.project.post.dto.PostAdminUpdateRequest;
import com.aivle.project.post.dto.PostResponse;
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
import org.springframework.security.access.prepost.PreAuthorize;
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
 * 관리자 전용 게시판(보드) CRUD API.
 */
@Tag(name = "게시글 (관리자)", description = "관리자용 보드 기반 게시글 CRUD API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/posts/{categoryName}")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPostController {

	private final PostService postService;

	@GetMapping
	@Operation(summary = "보드별 전체 게시글 조회 (관리자)", description = "관리자 권한으로 특정 보드의 모든 게시글(DRAFT, HIDDEN 포함)을 조회합니다.")
	public ResponseEntity<ApiResponse<PageResponse<PostResponse>>> list(
		@PathVariable String categoryName,
		@RequestParam(defaultValue = "1") int page,
		@RequestParam(defaultValue = "10") int size,
		@RequestParam(defaultValue = "createdAt") String sortBy,
		@RequestParam(defaultValue = "DESC") String direction
	) {
		PageRequest pageRequest = new PageRequest();
		pageRequest.setPage(page);
		pageRequest.setSize(size);
		pageRequest.setSortBy(sortBy);
		pageRequest.setDirection(Sort.Direction.valueOf(direction));
		return ResponseEntity.ok(ApiResponse.ok(postService.listAdmin(categoryName, pageRequest)));
	}

	@GetMapping("/{postId}")
	@Operation(summary = "게시글 상세 조회 (관리자)", description = "관리자 권한으로 특정 게시글의 상세 정보를 조회합니다.")
	public ResponseEntity<ApiResponse<PostResponse>> get(
		@PathVariable String categoryName,
		@PathVariable Long postId
	) {
		return ResponseEntity.ok(ApiResponse.ok(postService.getAdmin(categoryName, postId)));
	}

	@PostMapping
	@Operation(summary = "게시글 생성 (관리자)", description = "관리자 권한으로 특정 보드에 게시글을 생성합니다. (고정 여부, 상태 설정 가능)")
	public ResponseEntity<ApiResponse<PostResponse>> create(
		@PathVariable String categoryName,
		@CurrentUser UserEntity admin,
		@Valid @RequestBody PostAdminCreateRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.ok(postService.createAdmin(categoryName, admin, request)));
	}

	@PatchMapping("/{postId}")
	@Operation(summary = "게시글 수정 (관리자)", description = "관리자 권한으로 게시글 정보를 수정합니다. (고정 여부, 상태 설정 가능)")
	public ResponseEntity<ApiResponse<PostResponse>> update(
		@PathVariable String categoryName,
		@PathVariable Long postId,
		@Valid @RequestBody PostAdminUpdateRequest request
	) {
		return ResponseEntity.ok(ApiResponse.ok(postService.updateAdmin(categoryName, postId, request)));
	}

	@DeleteMapping("/{postId}")
	@Operation(summary = "게시글 삭제 (관리자)", description = "관리자 권한으로 게시글을 삭제합니다.")
	public ResponseEntity<ApiResponse<Void>> delete(
		@PathVariable String categoryName,
		@PathVariable Long postId
	) {
		postService.deleteAdmin(categoryName, postId);
		return ResponseEntity.ok(ApiResponse.ok());
	}
}
