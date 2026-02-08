package com.aivle.project.file.storage;

import com.aivle.project.file.config.FileStorageProperties;
import com.aivle.project.file.entity.FilesEntity;
import com.aivle.project.file.exception.FileErrorCode;
import com.aivle.project.file.exception.FileException;
import java.io.InputStream;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

/**
 * S3 파일 스트리밍 서비스 (prod 전용).
 */
@Component
@Profile("prod")
@RequiredArgsConstructor
public class S3FileStreamService implements FileStreamService {

	private final S3Client s3Client;
	private final FileStorageProperties properties;

	@Override
	public InputStream openStream(FilesEntity file) {
		if (file == null) {
			throw new FileException(FileErrorCode.FILE_404_NOT_FOUND);
		}
		String bucket = properties.getS3().getBucket();
		if (!StringUtils.hasText(bucket)) {
			throw new FileException(FileErrorCode.FILE_500_STORAGE);
		}
		String key = resolveStorageKey(file);
		if (!StringUtils.hasText(key)) {
			throw new FileException(FileErrorCode.FILE_404_NOT_FOUND);
		}

		GetObjectRequest request = GetObjectRequest.builder()
			.bucket(bucket)
			.key(key)
			.build();
		try {
			return s3Client.getObject(request);
		} catch (Exception ex) {
			throw new FileException(FileErrorCode.FILE_500_STORAGE);
		}
	}

	private String resolveStorageKey(FilesEntity file) {
		if (StringUtils.hasText(file.getStorageKey())) {
			return file.getStorageKey();
		}
		String storageUrl = file.getStorageUrl();
		if (!StringUtils.hasText(storageUrl)) {
			return "";
		}
		try {
			URI uri = URI.create(storageUrl);
			String path = uri.getPath();
			if (!StringUtils.hasText(path)) {
				return "";
			}
			return path.startsWith("/") ? path.substring(1) : path;
		} catch (IllegalArgumentException ex) {
			return "";
		}
	}
}
