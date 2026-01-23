package com.aivle.project.file.controller;

import com.aivle.project.common.security.CurrentUser;
import com.aivle.project.file.entity.FilesEntity;
import com.aivle.project.file.exception.FileErrorCode;
import com.aivle.project.file.exception.FileException;
import com.aivle.project.file.service.FileService;
import com.aivle.project.user.entity.UserEntity;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 파일 조회/다운로드 API.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/files")
public class FileDownloadController {

	private final FileService fileService;

	@GetMapping("/{fileId}")
	public ResponseEntity<?> download(
		@CurrentUser UserEntity user,
		@PathVariable Long fileId
	) {
		FilesEntity file = fileService.getFile(fileId, user);
		String storageUrl = file.getStorageUrl();

		if (storageUrl != null && storageUrl.startsWith("http")) {
			return ResponseEntity.status(HttpStatus.FOUND)
				.location(URI.create(storageUrl))
				.build();
		}

		if (!StringUtils.hasText(storageUrl)) {
			throw new FileException(FileErrorCode.FILE_404_NOT_FOUND);
		}

		try {
			Path path = Path.of(storageUrl);
			if (!Files.exists(path)) {
				throw new FileException(FileErrorCode.FILE_404_NOT_FOUND);
			}
			UrlResource resource = new UrlResource(path.toUri());
			String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
			return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_TYPE, contentType)
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getOriginalFilename() + "\"")
				.body(resource);
		} catch (Exception ex) {
			throw new FileException(FileErrorCode.FILE_500_STORAGE);
		}
	}
}
