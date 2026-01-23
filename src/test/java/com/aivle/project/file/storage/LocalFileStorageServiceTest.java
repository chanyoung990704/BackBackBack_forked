package com.aivle.project.file.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aivle.project.file.config.FileUploadProperties;
import com.aivle.project.file.exception.FileErrorCode;
import com.aivle.project.file.exception.FileException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class LocalFileStorageServiceTest {

	@TempDir
	Path tempDir;

	@Test
	@DisplayName("로컬 저장소에 파일이 저장된다")
	void store_shouldSaveFileToLocal() throws IOException {
		// given
		FileUploadProperties properties = new FileUploadProperties();
		properties.setUploadDir(tempDir.toString());
		LocalFileStorageService storageService = new LocalFileStorageService(properties);
		MultipartFile file = new MockMultipartFile(
			"file",
			"image.png",
			"image/png",
			"hello".getBytes(StandardCharsets.UTF_8)
		);

		// when
		StoredFile stored = storageService.store(file, "posts/1");

		// then
		Path savedPath = Path.of(stored.storageUrl().replace("/", java.io.File.separator));
		assertThat(Files.exists(savedPath)).isTrue();
		assertThat(Files.readAllBytes(savedPath)).isEqualTo("hello".getBytes(StandardCharsets.UTF_8));
	}

	@Test
	@DisplayName("업로드 경로가 없으면 예외가 발생한다")
	void store_shouldFailWhenBasePathMissing() {
		// given
		FileUploadProperties properties = new FileUploadProperties();
		properties.setUploadDir(" ");
		LocalFileStorageService storageService = new LocalFileStorageService(properties);
		MultipartFile file = new MockMultipartFile(
			"file",
			"image.png",
			"image/png",
			"hello".getBytes(StandardCharsets.UTF_8)
		);

		// when & then
		assertThatThrownBy(() -> storageService.store(file, "posts/1"))
			.isInstanceOf(FileException.class)
			.extracting(ex -> ((FileException) ex).getErrorCode())
			.isEqualTo(FileErrorCode.FILE_500_STORAGE);
	}

	@Test
	@DisplayName("빈 파일은 업로드할 수 없다")
	void store_shouldFailWhenFileEmpty() {
		// given
		FileUploadProperties properties = new FileUploadProperties();
		properties.setUploadDir(tempDir.toString());
		LocalFileStorageService storageService = new LocalFileStorageService(properties);
		MultipartFile file = new MockMultipartFile(
			"file",
			"image.png",
			"image/png",
			new byte[] {}
		);

		// when & then
		assertThatThrownBy(() -> storageService.store(file, "posts/1"))
			.isInstanceOf(FileException.class)
			.extracting(ex -> ((FileException) ex).getErrorCode())
			.isEqualTo(FileErrorCode.FILE_400_EMPTY);
	}
}
