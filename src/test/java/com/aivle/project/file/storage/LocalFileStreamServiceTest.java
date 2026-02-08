package com.aivle.project.file.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aivle.project.file.entity.FileUsageType;
import com.aivle.project.file.entity.FilesEntity;
import com.aivle.project.file.exception.FileErrorCode;
import com.aivle.project.file.exception.FileException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LocalFileStreamServiceTest {

	@Test
	@DisplayName("로컬 저장 파일을 스트리밍으로 연다")
	void openStream_shouldReturnStream() throws Exception {
		// given
		Path tempFile = Files.createTempFile("local-file-stream", ".txt");
		Files.writeString(tempFile, "hello", StandardCharsets.UTF_8);
		LocalFileStreamService service = new LocalFileStreamService();
		FilesEntity file = FilesEntity.create(
			FileUsageType.POST_ATTACHMENT,
			tempFile.toString(),
			null,
			"sample.txt",
			5L,
			"text/plain"
		);

		// when
		try (InputStream inputStream = service.openStream(file)) {
			byte[] bytes = inputStream.readAllBytes();

			// then
			assertThat(new String(bytes, StandardCharsets.UTF_8)).isEqualTo("hello");
		}
	}

	@Test
	@DisplayName("로컬 파일이 없으면 404 예외를 던진다")
	void openStream_shouldThrowWhenFileMissing() {
		// given
		LocalFileStreamService service = new LocalFileStreamService();
		FilesEntity file = FilesEntity.create(
			FileUsageType.POST_ATTACHMENT,
			Path.of("does-not-exist.txt").toString(),
			null,
			"missing.txt",
			0L,
			"text/plain"
		);

		// when & then
		assertThatThrownBy(() -> service.openStream(file))
			.isInstanceOf(FileException.class)
			.hasMessageContaining(FileErrorCode.FILE_404_NOT_FOUND.getMessage());
	}
}
