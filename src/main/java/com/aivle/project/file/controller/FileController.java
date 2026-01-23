package com.aivle.project.file.controller;

import com.aivle.project.common.dto.ApiResponse;
import com.aivle.project.common.security.CurrentUser;
import com.aivle.project.file.dto.FileResponse;
import com.aivle.project.file.service.FileService;
import com.aivle.project.user.entity.UserEntity;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
@RestController
@RequiredArgsConstructor
@RequestMapping("/posts")
public class FileController {

	private final FileService fileService;

	@PostMapping("/{postId}/files")
	public ResponseEntity<ApiResponse<List<FileResponse>>> upload(
		@CurrentUser UserEntity user,
		@PathVariable Long postId,
		@RequestPart("files") List<MultipartFile> files
	) {
		List<FileResponse> responses = fileService.upload(postId, user, files);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(responses));
	}

	@GetMapping("/{postId}/files")
	public ResponseEntity<ApiResponse<List<FileResponse>>> list(
		@CurrentUser UserEntity user,
		@PathVariable Long postId
	) {
		List<FileResponse> responses = fileService.list(postId, user);
		return ResponseEntity.ok(ApiResponse.ok(responses));
	}
}
