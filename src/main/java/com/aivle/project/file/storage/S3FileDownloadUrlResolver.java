package com.aivle.project.file.storage;

import com.aivle.project.file.config.FileStorageProperties;
import com.aivle.project.file.entity.FilesEntity;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

/**
 * S3 다운로드 URL 해석기 (prod 전용).
 */
@Component
@Profile("prod")
@RequiredArgsConstructor
public class S3FileDownloadUrlResolver implements FileDownloadUrlResolver {

	private static final Duration PRESIGN_DURATION = Duration.ofMinutes(10);

	private final S3Presigner presigner;
	private final FileStorageProperties properties;

	@Override
	public Optional<String> resolve(FilesEntity file) {
		if (file == null) {
			return Optional.empty();
		}
		String storageUrl = file.getStorageUrl();
		if (!StringUtils.hasText(storageUrl) || !storageUrl.startsWith("http")) {
			return Optional.empty();
		}

		String bucket = properties.getS3().getBucket();
		if (!StringUtils.hasText(bucket)) {
			return Optional.empty();
		}

		String key = resolveStorageKey(file);
		if (!StringUtils.hasText(key)) {
			return Optional.empty();
		}

		GetObjectRequest getObjectRequest = GetObjectRequest.builder()
			.bucket(bucket)
			.key(key)
			.build();
		PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(
			GetObjectPresignRequest.builder()
				.getObjectRequest(getObjectRequest)
				.signatureDuration(PRESIGN_DURATION)
				.build()
		);
		return Optional.of(presignedRequest.url().toString());
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
