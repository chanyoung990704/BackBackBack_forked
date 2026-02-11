package com.aivle.project.file.storage;

import com.aivle.project.file.config.FileUploadProperties;
import com.aivle.project.file.exception.FileErrorCode;
import com.aivle.project.file.exception.FileException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * perf 프로파일용 로컬 파일 저장소.
 */
@Component
@Profile("perf")
@RequiredArgsConstructor
public class PerfFileStorageService implements FileStorageService {

	private final FileUploadProperties properties;

	@Override
	public StoredFile store(MultipartFile file, String keyPrefix) {
		validateFile(file);
		String basePathValue = requireBasePath();
		String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
		String extension = resolveExtension(originalFilename);
		String storedName = UUID.randomUUID() + extension;

		Path basePath = Path.of(basePathValue);
		Path targetDir = keyPrefix == null || keyPrefix.isBlank()
			? basePath
			: basePath.resolve(keyPrefix);

		try {
			Files.createDirectories(targetDir);
			Path target = targetDir.resolve(storedName).normalize();
			Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
			String storageUrl = target.toString().replace("\\", "/");
			return new StoredFile(storageUrl, originalFilename, file.getSize(), file.getContentType(), null);
		} catch (IOException ex) {
			throw new FileException(FileErrorCode.FILE_500_STORAGE);
		}
	}

	private void validateFile(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new FileException(FileErrorCode.FILE_400_EMPTY);
		}
	}

	private String requireBasePath() {
		String basePath = properties.getUploadDir();
		if (!StringUtils.hasText(basePath)) {
			throw new FileException(FileErrorCode.FILE_500_STORAGE);
		}
		return basePath;
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
}
