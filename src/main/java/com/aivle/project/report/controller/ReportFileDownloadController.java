package com.aivle.project.report.controller;

import com.aivle.project.common.dto.ApiResponse;
import com.aivle.project.file.dto.FileDownloadUrlResponse;
import com.aivle.project.file.entity.FileUsageType;
import com.aivle.project.file.entity.FilesEntity;
import com.aivle.project.file.exception.FileErrorCode;
import com.aivle.project.file.exception.FileException;
import com.aivle.project.file.repository.FilesRepository;
import com.aivle.project.file.storage.FileStreamService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * 보고서 PDF 다운로드 API.
 */
//@Tag(name = "보고서 지표", description = "보고서 PDF 다운로드")
//@Controller
//@RequiredArgsConstructor
//@RequestMapping("/api/reports/files")
//@SecurityRequirement(name = "bearerAuth")
//public class ReportFileDownloadController {
//
//	private final FilesRepository filesRepository;
//	private final FileStreamService fileStreamService;
//
//	@GetMapping("/{fileId}")
//	@Operation(summary = "보고서 PDF 다운로드", description = "보고서 PDF를 스트리밍으로 다운로드합니다.")
//	public ResponseEntity<?> download(@PathVariable Long fileId) {
//		FilesEntity file = getReportPdf(fileId);
//		InputStream stream = fileStreamService.openStream(file);
//		String contentType = file.getContentType() != null ? file.getContentType() : MediaType.APPLICATION_PDF_VALUE;
//		String encodedFilename = URLEncoder.encode(file.getOriginalFilename(), StandardCharsets.UTF_8)
//			.replace("+", "%20");
//		return ResponseEntity.ok()
//			.header(HttpHeaders.CONTENT_TYPE, contentType)
//			.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFilename + "\"")
//			.header(HttpHeaders.CACHE_CONTROL, "private, no-store")
//			.contentLength(file.getFileSize())
//			.body(new InputStreamResource(stream));
//	}
//
//	@GetMapping("/{fileId}/url")
//	@Operation(summary = "보고서 PDF 다운로드 URL 조회", description = "보고서 PDF 다운로드 URL을 반환합니다.")
//	public ResponseEntity<ApiResponse<FileDownloadUrlResponse>> downloadUrl(@PathVariable Long fileId) {
//		getReportPdf(fileId);
//		String resolvedUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
//			.path("/api/reports/files/")
//			.path(fileId.toString())
//			.toUriString();
//		return ResponseEntity.ok(ApiResponse.ok(new FileDownloadUrlResponse(resolvedUrl)));
//	}
//
//	private FilesEntity getReportPdf(Long fileId) {
//		FilesEntity file = filesRepository.findById(fileId)
//			.orElseThrow(() -> new FileException(FileErrorCode.FILE_404_NOT_FOUND));
//		if (file.isDeleted() || file.getUsageType() != FileUsageType.REPORT_PDF) {
//			throw new FileException(FileErrorCode.FILE_404_NOT_FOUND);
//		}
//		return file;
//	}
//}
