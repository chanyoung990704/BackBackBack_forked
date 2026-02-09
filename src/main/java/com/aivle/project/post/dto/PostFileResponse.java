package com.aivle.project.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 게시글 첨부파일 응답 DTO.
 */
@Schema(description = "게시글 첨부파일 응답")
public record PostFileResponse(
	@Schema(description = "파일 ID", example = "1")
	Long id,
	@Schema(description = "원본 파일명", example = "document.pdf")
	String originalFilename,
	@Schema(description = "파일 크기(바이트)", example = "102400")
	long fileSize,
	@Schema(description = "콘텐츠 타입", example = "application/pdf")
	String contentType,
	@Schema(description = "업로드 일시", example = "2026-01-25T12:34:56")
	LocalDateTime createdAt,
	@Schema(description = "다운로드 가능 여부", example = "true")
	boolean downloadable,
	@Schema(description = "다운로드 URL", example = "/api/files/1")
	String downloadUrl
) {
}
