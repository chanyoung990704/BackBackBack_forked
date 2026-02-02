package com.aivle.project.report.controller;

import com.aivle.project.common.dto.ApiResponse;
import com.aivle.project.file.dto.FileDownloadUrlResponse;
import com.aivle.project.file.entity.FileUsageType;
import com.aivle.project.file.entity.FilesEntity;
import com.aivle.project.file.exception.FileErrorCode;
import com.aivle.project.file.exception.FileException;
import com.aivle.project.file.repository.FilesRepository;
import com.aivle.project.file.storage.FileDownloadUrlResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * 보고서 PDF 다운로드 API.
 */
@Tag(name = "보고서 지표", description = "보고서 PDF 다운로드")
@Controller
@RequiredArgsConstructor
@RequestMapping("/api/reports/files")
public class ReportFileDownloadController {

	private final FilesRepository filesRepository;
	private final FileDownloadUrlResolver fileDownloadUrlResolver;

	@GetMapping("/{fileId}")
	@Operation(summary = "보고서 PDF 다운로드", description = "보고서 PDF를 다운로드하거나 외부 URL로 리다이렉트합니다.")
	public ResponseEntity<?> download(@PathVariable Long fileId) {
		FilesEntity file = getReportPdf(fileId);

		String storageUrl = file.getStorageUrl();
		if (storageUrl != null && storageUrl.startsWith("http")) {
			String redirectUrl = fileDownloadUrlResolver.resolve(file)
				.orElse(storageUrl);
			return ResponseEntity.status(HttpStatus.FOUND)
				.location(URI.create(redirectUrl))
				.build();
		}
		if (storageUrl != null && storageUrl.startsWith("memory://")) {
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

	@GetMapping("/{fileId}/url")
	@Operation(summary = "보고서 PDF 다운로드 URL 조회", description = "보고서 PDF 다운로드 URL(프리사인드 포함)을 반환합니다.")
	public ResponseEntity<ApiResponse<FileDownloadUrlResponse>> downloadUrl(@PathVariable Long fileId) {
		FilesEntity file = getReportPdf(fileId);
		String resolvedUrl = resolveDownloadUrl(file, fileId);
		if (!StringUtils.hasText(resolvedUrl)) {
			throw new FileException(FileErrorCode.FILE_404_NOT_FOUND);
		}
		return ResponseEntity.ok(ApiResponse.ok(new FileDownloadUrlResponse(resolvedUrl)));
	}

	private FilesEntity getReportPdf(Long fileId) {
		FilesEntity file = filesRepository.findById(fileId)
			.orElseThrow(() -> new FileException(FileErrorCode.FILE_404_NOT_FOUND));
		if (file.isDeleted() || file.getUsageType() != FileUsageType.REPORT_PDF) {
			throw new FileException(FileErrorCode.FILE_404_NOT_FOUND);
		}
		return file;
	}

	private String resolveDownloadUrl(FilesEntity file, Long fileId) {
		String storageUrl = file.getStorageUrl();
		if (!StringUtils.hasText(storageUrl)) {
			return null;
		}
		if (storageUrl.startsWith("http")) {
			return fileDownloadUrlResolver.resolve(file).orElse(storageUrl);
		}
		if (storageUrl.startsWith("memory://")) {
			return storageUrl;
		}
		return ServletUriComponentsBuilder.fromCurrentContextPath()
			.path("/reports/files/")
			.path(fileId.toString())
			.toUriString();
	}
}
