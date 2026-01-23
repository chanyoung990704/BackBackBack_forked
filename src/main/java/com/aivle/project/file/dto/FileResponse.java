package com.aivle.project.file.dto;

import com.aivle.project.file.entity.FilesEntity;
import java.time.LocalDateTime;

/**
 * 파일 업로드 응답 DTO.
 */
public record FileResponse(
	Long id,
	Long postId,
	String storageUrl,
	String originalFilename,
	long fileSize,
	String contentType,
	LocalDateTime createdAt
) {
	public static FileResponse from(FilesEntity entity) {
		return new FileResponse(
			entity.getId(),
			entity.getPost().getId(),
			entity.getStorageUrl(),
			entity.getOriginalFilename(),
			entity.getFileSize(),
			entity.getContentType(),
			entity.getCreatedAt()
		);
	}
}
