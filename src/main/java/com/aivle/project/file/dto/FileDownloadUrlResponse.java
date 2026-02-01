package com.aivle.project.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 파일 다운로드 URL 응답 DTO.
 */
@Schema(description = "파일 다운로드 URL 응답")
public record FileDownloadUrlResponse(
	@Schema(description = "다운로드 URL", example = "https://cdn.example.com/files/1")
	String url
) {
}
