package com.aivle.project.post.controller;

import com.aivle.project.common.dto.ApiResponse;
import com.aivle.project.common.dto.PageRequest;
import com.aivle.project.common.dto.PageResponse;
import com.aivle.project.common.security.CurrentUser;
import com.aivle.project.post.dto.PostCreateRequest;
import com.aivle.project.post.dto.PostResponse;
import com.aivle.project.post.dto.PostUpdateRequest;
import com.aivle.project.post.service.PostService;
import com.aivle.project.user.entity.UserEntity;
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
 * 사용자 게시글 CRUD API.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/posts")
public class PostController {

	private final PostService postService;

	@GetMapping
	public ResponseEntity<ApiResponse<PageResponse<PostResponse>>> list(
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
		return ResponseEntity.ok(ApiResponse.ok(postService.list(pageRequest)));
	}

	@GetMapping("/{postId}")
	public ResponseEntity<ApiResponse<PostResponse>> get(@PathVariable Long postId) {
		return ResponseEntity.ok(ApiResponse.ok(postService.get(postId)));
	}

	@PostMapping
	public ResponseEntity<ApiResponse<PostResponse>> create(
		@CurrentUser UserEntity user,
		@Valid @RequestBody PostCreateRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.ok(postService.create(user, request)));
	}

	@PatchMapping("/{postId}")
	public ResponseEntity<ApiResponse<PostResponse>> update(
		@CurrentUser UserEntity user,
		@PathVariable Long postId,
		@Valid @RequestBody PostUpdateRequest request
	) {
		return ResponseEntity.ok(ApiResponse.ok(postService.update(user, postId, request)));
	}

	@DeleteMapping("/{postId}")
	public ResponseEntity<ApiResponse<Void>> delete(
		@CurrentUser UserEntity user,
		@PathVariable Long postId
	) {
		postService.delete(user, postId);
		return ResponseEntity.ok(ApiResponse.ok());
	}
}
