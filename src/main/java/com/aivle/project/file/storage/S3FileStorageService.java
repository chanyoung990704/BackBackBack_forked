package com.aivle.project.file.storage;

import com.aivle.project.file.config.FileStorageProperties;
import com.aivle.project.file.exception.FileErrorCode;
import com.aivle.project.file.exception.FileException;
import java.io.IOException;
import java.net.URL;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * S3 파일 저장소 (prod 전용).
 */
@Component
@Profile("prod")
@RequiredArgsConstructor
public class S3FileStorageService implements FileStorageService {

	private final S3Client s3Client;
	private final FileStorageProperties properties;

	@Override
	public StoredFile store(MultipartFile file, String keyPrefix) {
		validateFile(file);
		String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
		String extension = resolveExtension(originalFilename);
		String storedName = UUID.randomUUID() + extension;
		String key = buildKey(keyPrefix, storedName);

		try {
			PutObjectRequest request = PutObjectRequest.builder()
				.bucket(properties.getS3().getBucket())
				.key(key)
				.contentType(file.getContentType())
				.build();
			s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

			URL url = s3Client.utilities().getUrl(GetUrlRequest.builder()
				.bucket(properties.getS3().getBucket())
				.key(key)
				.build());

			return new StoredFile(url.toString(), originalFilename, file.getSize(), file.getContentType(), key);
		} catch (IOException ex) {
			throw new FileException(FileErrorCode.FILE_500_STORAGE);
		}
	}

	private void validateFile(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new FileException(FileErrorCode.FILE_400_EMPTY);
		}
	}

	private String resolveExtension(String filename) {
		if (filename == null || filename.isBlank()) {
			return "";
		}
		int index = filename.lastIndexOf('.');
		if (index < 0 || index == filename.length() - 1) {
			return "";
		}
		return filename.substring(index);
	}

	private String buildKey(String keyPrefix, String storedName) {
		String basePrefix = properties.getS3().getPrefix();
		String normalizedPrefix = normalizePath(basePrefix);
		String normalizedKeyPrefix = normalizePath(keyPrefix);

		StringBuilder builder = new StringBuilder();
		if (!normalizedPrefix.isBlank()) {
			builder.append(normalizedPrefix);
		}
		if (!normalizedKeyPrefix.isBlank()) {
			if (builder.length() > 0) {
				builder.append('/');
			}
			builder.append(normalizedKeyPrefix);
		}
		if (builder.length() > 0) {
			builder.append('/');
		}
		builder.append(storedName);
		return builder.toString();
	}

	private String normalizePath(String path) {
		if (path == null) {
			return "";
		}
		String trimmed = path.trim().replace("\\", "/");
		while (trimmed.startsWith("/")) {
			trimmed = trimmed.substring(1);
		}
		while (trimmed.endsWith("/")) {
			trimmed = trimmed.substring(0, trimmed.length() - 1);
		}
		return trimmed;
	}
}
