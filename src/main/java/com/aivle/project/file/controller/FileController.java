package com.aivle.project.file.controller;

import com.aivle.project.common.dto.ApiResponse;
import com.aivle.project.common.security.CurrentUser;
import com.aivle.project.file.dto.FileResponse;
import com.aivle.project.file.service.FileService;
import com.aivle.project.user.entity.UserEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 파일 업로드 API.
 */
@Tag(name = "파일", description = "파일 업로드/목록 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class FileController {

	private final FileService fileService;

	@PostMapping(value = "/{postId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "파일 업로드", description = "게시글에 파일을 업로드합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "업로드 성공",
			content = @Content(schema = @Schema(implementation = ApiResponse.class))),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청값 오류"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 없음"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
	})
	public ResponseEntity<ApiResponse<List<FileResponse>>> upload(
		@Parameter(hidden = true) @CurrentUser UserEntity user,
		@Parameter(description = "게시글 ID", example = "100")
		@PathVariable Long postId,
		@Parameter(description = "업로드 파일 목록")
		@RequestPart("files") List<MultipartFile> files
	) {
		List<FileResponse> responses = fileService.upload(postId, user, files);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(responses));
	}

	@GetMapping("/{postId}/files")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "파일 목록 조회", description = "게시글에 업로드된 파일 목록을 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
			content = @Content(schema = @Schema(implementation = ApiResponse.class))),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 없음"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
	})
	public ResponseEntity<ApiResponse<List<FileResponse>>> list(
		@Parameter(hidden = true) @CurrentUser UserEntity user,
		@Parameter(description = "게시글 ID", example = "100")
		@PathVariable Long postId
	) {
		List<FileResponse> responses = fileService.list(postId, user);
		return ResponseEntity.ok(ApiResponse.ok(responses));
	}
}
