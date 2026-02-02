package com.aivle.project.file.storage;

/**
 * 저장된 파일 메타데이터.
 */
public record StoredFile(
	String storageUrl,
	String originalFilename,
	long fileSize,
	String contentType,
	String storageKey
) {
}
