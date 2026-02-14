package com.aivle.project.comment.controller;

import com.aivle.project.comment.dto.CommentCreateRequest;
import com.aivle.project.comment.dto.CommentResponse;
import com.aivle.project.comment.dto.CommentUpdateRequest;
import com.aivle.project.comment.service.CommentsService;
import com.aivle.project.common.dto.ApiResponse;
import com.aivle.project.common.security.CurrentUser;
import com.aivle.project.user.entity.UserEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "댓글", description = "댓글 CRUD API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CommentController {

	private final CommentsService commentsService;

	@GetMapping("/posts/{postId}/comments")
	@Operation(summary = "댓글 목록 조회", description = "게시글 댓글 목록을 조회합니다.", security = {})
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
			content = @Content(schema = @Schema(implementation = ApiResponse.class))),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 없음"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
	})
	public ResponseEntity<ApiResponse<List<CommentResponse>>> list(
		@Parameter(description = "게시글 ID", example = "100")
		@PathVariable Long postId
	) {
		return ResponseEntity.ok(ApiResponse.ok(commentsService.listByPost(postId)));
	}

	@PostMapping("/posts/{postId}/comments")
	@Operation(summary = "댓글 생성", description = "게시글에 댓글을 작성합니다.")
	@SecurityRequirement(name = "bearerAuth")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공",
			content = @Content(schema = @Schema(implementation = ApiResponse.class))),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청값 오류"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 없음"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
	})
	public ResponseEntity<ApiResponse<CommentResponse>> create(
		@CurrentUser UserEntity user,
		@Parameter(description = "게시글 ID", example = "100")
		@PathVariable Long postId,
		@Valid @RequestBody CommentCreateRequest request
	) {
		request.setPostId(postId);
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.ok(commentsService.create(user, request)));
	}

	@PatchMapping("/comments/{commentId}")
	@Operation(summary = "댓글 수정", description = "댓글 내용을 수정합니다.")
	@SecurityRequirement(name = "bearerAuth")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공",
			content = @Content(schema = @Schema(implementation = ApiResponse.class))),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청값 오류"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "댓글 없음"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
	})
	public ResponseEntity<ApiResponse<CommentResponse>> update(
		@CurrentUser UserEntity user,
		@Parameter(description = "댓글 ID", example = "10")
		@PathVariable Long commentId,
		@Valid @RequestBody CommentUpdateRequest request
	) {
		return ResponseEntity.ok(ApiResponse.ok(commentsService.update(user.getId(), commentId, request)));
	}

	@DeleteMapping("/comments/{commentId}")
	@Operation(summary = "댓글 삭제", description = "댓글을 삭제합니다.")
	@SecurityRequirement(name = "bearerAuth")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공",
			content = @Content(schema = @Schema(implementation = ApiResponse.class))),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "댓글 없음"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
	})
	public ResponseEntity<ApiResponse<Void>> delete(
		@CurrentUser UserEntity user,
		@Parameter(description = "댓글 ID", example = "10")
		@PathVariable Long commentId
	) {
		commentsService.delete(user.getId(), commentId);
		return ResponseEntity.ok(ApiResponse.ok());
	}
}
