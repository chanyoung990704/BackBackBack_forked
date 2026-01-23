package com.aivle.project.file.validator;

import com.aivle.project.file.config.FileUploadProperties;
import com.aivle.project.file.exception.FileErrorCode;
import com.aivle.project.file.exception.FileException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 파일 업로드 검증.
 */
@Component
@RequiredArgsConstructor
public class FileValidator {

	private static final Map<String, byte[]> FILE_SIGNATURES = Map.of(
		"image/png", new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A},
		"image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
		"application/pdf", new byte[] {0x25, 0x50, 0x44, 0x46}
	);

	private static final byte[] GIF_SIGNATURE_87A = new byte[] {0x47, 0x49, 0x46, 0x38, 0x37, 0x61};
	private static final byte[] GIF_SIGNATURE_89A = new byte[] {0x47, 0x49, 0x46, 0x38, 0x39, 0x61};

	private final FileUploadProperties properties;

	public void validateMultiple(List<MultipartFile> files) {
		if (files == null || files.isEmpty()) {
			throw new FileException(FileErrorCode.FILE_400_EMPTY);
		}
		if (files.size() > properties.getMaxFiles()) {
			throw new FileException(FileErrorCode.FILE_400_TOO_MANY);
		}
		long totalSize = 0;
		for (MultipartFile file : files) {
			validate(file);
			totalSize += file.getSize();
		}
		long maxTotalSize = properties.getMaxSize() * properties.getMaxFiles();
		if (totalSize > maxTotalSize) {
			throw new FileException(FileErrorCode.FILE_400_TOTAL_SIZE);
		}
	}

	public void validate(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new FileException(FileErrorCode.FILE_400_EMPTY);
		}
		if (file.getSize() > properties.getMaxSize()) {
			throw new FileException(FileErrorCode.FILE_400_SIZE);
		}
		validateExtension(file);
		validateContentType(file);
		if (properties.isValidateSignature()) {
			validateSignature(file);
		}
	}

	private void validateExtension(MultipartFile file) {
		String filename = file.getOriginalFilename();
		if (!StringUtils.hasText(filename)) {
			throw new FileException(FileErrorCode.FILE_400_EXTENSION);
		}
		String extension = resolveExtension(filename);
		if (extension == null) {
			throw new FileException(FileErrorCode.FILE_400_EXTENSION);
		}
		if (!properties.getAllowedExtensions().isEmpty()
			&& properties.getAllowedExtensions().stream()
			.noneMatch(allowed -> allowed.equalsIgnoreCase(extension))) {
			throw new FileException(FileErrorCode.FILE_400_EXTENSION);
		}
	}

	private void validateContentType(MultipartFile file) {
		String contentType = file.getContentType();
		if (!properties.getAllowedContentTypes().isEmpty()
			&& (contentType == null || !properties.getAllowedContentTypes().contains(contentType))) {
			throw new FileException(FileErrorCode.FILE_400_CONTENT_TYPE);
		}
	}

	private void validateSignature(MultipartFile file) {
		String contentType = file.getContentType();
		if (contentType == null) {
			throw new FileException(FileErrorCode.FILE_400_SIGNATURE);
		}
		try (InputStream inputStream = file.getInputStream()) {
			byte[] header = new byte[8];
			int bytesRead = inputStream.read(header);
			if (bytesRead < 4) {
				throw new FileException(FileErrorCode.FILE_400_SIGNATURE);
			}

			boolean isValid = false;
			if ("image/gif".equals(contentType)) {
				isValid = matchesSignature(header, GIF_SIGNATURE_87A)
					|| matchesSignature(header, GIF_SIGNATURE_89A);
			} else if (FILE_SIGNATURES.containsKey(contentType)) {
				isValid = matchesSignature(header, FILE_SIGNATURES.get(contentType));
			} else {
				return;
			}

			if (!isValid) {
				throw new FileException(FileErrorCode.FILE_400_SIGNATURE);
			}
		} catch (IOException ex) {
			throw new FileException(FileErrorCode.FILE_500_SIGNATURE);
		}
	}

	private boolean matchesSignature(byte[] fileHeader, byte[] signature) {
		if (fileHeader.length < signature.length) {
			return false;
		}
		for (int i = 0; i < signature.length; i++) {
			if (fileHeader[i] != signature[i]) {
				return false;
			}
		}
		return true;
	}

	private String resolveExtension(String filename) {
		int index = filename.lastIndexOf('.');
		if (index < 0 || index == filename.length() - 1) {
			return null;
		}
		return filename.substring(index + 1).toLowerCase();
	}
}
