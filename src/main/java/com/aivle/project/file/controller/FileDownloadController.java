package com.aivle.project.file.controller;

import com.aivle.project.common.dto.ApiResponse;
import com.aivle.project.common.security.CurrentUser;
import com.aivle.project.file.dto.FileDownloadUrlResponse;
import com.aivle.project.file.entity.FilesEntity;
import com.aivle.project.file.service.FileService;
import com.aivle.project.file.storage.FileStreamService;
import com.aivle.project.user.entity.UserEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * 파일 조회/다운로드 API.
 */
@Tag(name = "파일", description = "파일 다운로드 API")
@Controller
@RequiredArgsConstructor
@RequestMapping("/api/files")
public class FileDownloadController {

private final FileService fileService;
	private final FileStreamService fileStreamService;

	@GetMapping("/{fileId}")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "파일 다운로드", description = "파일을 스트리밍으로 다운로드합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "다운로드 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "파일 없음"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
	})
	public ResponseEntity<?> download(
		@CurrentUser UserEntity user,
		@Parameter(description = "파일 ID", example = "1")
		@PathVariable Long fileId
	) {
		FilesEntity file = fileService.getFile(fileId, user);
		InputStream stream = fileStreamService.openStream(file);
		String contentType = file.getContentType() != null ? file.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
		String encodedFilename = URLEncoder.encode(file.getOriginalFilename(), StandardCharsets.UTF_8)
			.replace("+", "%20");
		return ResponseEntity.ok()
			.header(HttpHeaders.CONTENT_TYPE, contentType)
			.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFilename + "\"")
			.header(HttpHeaders.CACHE_CONTROL, "private, no-store")
			.contentLength(file.getFileSize())
			.body(new InputStreamResource(stream));
	}

	@GetMapping("/{fileId}/url")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "파일 다운로드 URL 조회", description = "파일 다운로드 URL을 반환합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "URL 조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "파일 없음"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
	})
	public ResponseEntity<ApiResponse<FileDownloadUrlResponse>> downloadUrl(
		@CurrentUser UserEntity user,
		@Parameter(description = "파일 ID", example = "1")
		@PathVariable Long fileId
	) {
		fileService.getFile(fileId, user);
		String resolvedUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
			.path("/api/files/")
			.path(fileId.toString())
			.toUriString();
		return ResponseEntity.ok(ApiResponse.ok(new FileDownloadUrlResponse(resolvedUrl)));
	}
}
