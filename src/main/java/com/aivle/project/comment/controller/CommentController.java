package com.aivle.project.comment.controller;

import com.aivle.project.comment.dto.CommentCreateRequest;
import com.aivle.project.comment.dto.CommentResponse;
import com.aivle.project.comment.dto.CommentUpdateRequest;
import com.aivle.project.comment.service.CommentsService;
import com.aivle.project.common.dto.ApiResponse;
import com.aivle.project.common.security.CurrentUser;
import com.aivle.project.user.entity.UserEntity;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 게시글 댓글 CRUD API.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping
public class CommentController {

	private final CommentsService commentsService;

	@GetMapping("/posts/{postId}/comments")
	public ResponseEntity<ApiResponse<List<CommentResponse>>> list(@PathVariable Long postId) {
		return ResponseEntity.ok(ApiResponse.ok(commentsService.listByPost(postId)));
	}

	@PostMapping("/posts/{postId}/comments")
	public ResponseEntity<ApiResponse<CommentResponse>> create(
		@CurrentUser UserEntity user,
		@PathVariable Long postId,
		@Valid @RequestBody CommentCreateRequest request
	) {
		request.setPostId(postId);
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.ok(commentsService.create(user, request)));
	}

	@PatchMapping("/comments/{commentId}")
	public ResponseEntity<ApiResponse<CommentResponse>> update(
		@CurrentUser UserEntity user,
		@PathVariable Long commentId,
		@Valid @RequestBody CommentUpdateRequest request
	) {
		return ResponseEntity.ok(ApiResponse.ok(commentsService.update(user.getId(), commentId, request)));
	}

	@DeleteMapping("/comments/{commentId}")
	public ResponseEntity<ApiResponse<Void>> delete(
		@CurrentUser UserEntity user,
		@PathVariable Long commentId
	) {
		commentsService.delete(user.getId(), commentId);
		return ResponseEntity.ok(ApiResponse.ok());
	}
}
