package com.aivle.project.file.validator;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aivle.project.file.config.FileUploadProperties;
import com.aivle.project.file.exception.FileErrorCode;
import com.aivle.project.file.exception.FileException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class FileValidatorTest {

	private FileValidator fileValidator;
	private FileUploadProperties properties;

	@BeforeEach
	void setUp() {
		properties = new FileUploadProperties();
		properties.setMaxSize(5 * 1024L);
		properties.setMaxFiles(2);
		properties.setAllowedContentTypes(List.of("image/png", "image/jpeg", "application/pdf"));
		properties.setAllowedExtensions(List.of("png", "jpg", "jpeg", "pdf"));
		properties.setValidateSignature(false);

		fileValidator = new FileValidator(properties);
	}

	@Test
	@DisplayName("정상 파일은 검증을 통과한다")
	void validate_shouldPassForValidFile() {
		// given
		MultipartFile file = new MockMultipartFile(
			"file",
			"image.png",
			"image/png",
			new byte[] {1, 2, 3}
		);

		// when & then
		assertThatCode(() -> fileValidator.validate(file)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("확장자가 허용되지 않으면 예외가 발생한다")
	void validate_shouldFailForInvalidExtension() {
		// given
		MultipartFile file = new MockMultipartFile(
			"file",
			"image.exe",
			"image/png",
			new byte[] {1, 2, 3}
		);

		// when & then
		assertThatThrownBy(() -> fileValidator.validate(file))
			.isInstanceOf(FileException.class)
			.extracting(ex -> ((FileException) ex).getErrorCode())
			.isEqualTo(FileErrorCode.FILE_400_EXTENSION);
	}

	@Test
	@DisplayName("컨텐츠 타입이 허용되지 않으면 예외가 발생한다")
	void validate_shouldFailForInvalidContentType() {
		// given
		MultipartFile file = new MockMultipartFile(
			"file",
			"image.png",
			"application/octet-stream",
			new byte[] {1, 2, 3}
		);

		// when & then
		assertThatThrownBy(() -> fileValidator.validate(file))
			.isInstanceOf(FileException.class)
			.extracting(ex -> ((FileException) ex).getErrorCode())
			.isEqualTo(FileErrorCode.FILE_400_CONTENT_TYPE);
	}

	@Test
	@DisplayName("파일 크기가 제한을 초과하면 예외가 발생한다")
	void validate_shouldFailForSizeLimit() {
		// given
		byte[] payload = new byte[6 * 1024];
		MultipartFile file = new MockMultipartFile("file", "image.png", "image/png", payload);

		// when & then
		assertThatThrownBy(() -> fileValidator.validate(file))
			.isInstanceOf(FileException.class)
			.extracting(ex -> ((FileException) ex).getErrorCode())
			.isEqualTo(FileErrorCode.FILE_400_SIZE);
	}

	@Test
	@DisplayName("파일 개수가 제한을 초과하면 예외가 발생한다")
	void validateMultiple_shouldFailForTooManyFiles() {
		// given
		MultipartFile first = new MockMultipartFile("file", "image.png", "image/png", new byte[] {1});
		MultipartFile second = new MockMultipartFile("file", "image2.png", "image/png", new byte[] {1});
		MultipartFile third = new MockMultipartFile("file", "image3.png", "image/png", new byte[] {1});

		// when & then
		assertThatThrownBy(() -> fileValidator.validateMultiple(List.of(first, second, third)))
			.isInstanceOf(FileException.class)
			.extracting(ex -> ((FileException) ex).getErrorCode())
			.isEqualTo(FileErrorCode.FILE_400_TOO_MANY);
	}

	@Test
	@DisplayName("시그니처 검증이 실패하면 예외가 발생한다")
	void validate_shouldFailForInvalidSignature() {
		// given
		properties.setValidateSignature(true);
		MultipartFile file = new MockMultipartFile(
			"file",
			"image.png",
			"image/png",
			new byte[] {0x00, 0x01, 0x02, 0x03}
		);

		// when & then
		assertThatThrownBy(() -> fileValidator.validate(file))
			.isInstanceOf(FileException.class)
			.extracting(ex -> ((FileException) ex).getErrorCode())
			.isEqualTo(FileErrorCode.FILE_400_SIGNATURE);
	}
}
