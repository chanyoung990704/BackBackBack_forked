package com.aivle.project.file.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
	classes = FileStorageConfig.class,
	webEnvironment = SpringBootTest.WebEnvironment.NONE,
	properties = {
		"app.file.upload.max-size=12345",
		"app.file.upload.max-files=7",
		"app.file.upload.allowed-content-types[0]=image/png",
		"app.file.upload.allowed-content-types[1]=application/pdf",
		"app.file.upload.allowed-extensions[0]=png",
		"app.file.upload.allowed-extensions[1]=pdf",
		"app.file.upload.validate-signature=true",
		"app.file.upload.upload-dir=/tmp/uploads-test",
		"spring.servlet.multipart.max-file-size=2MB",
		"spring.servlet.multipart.max-request-size=3MB",
		"spring.autoconfigure.exclude="
			+ "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
			+ "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,"
			+ "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration"
	}
)
@ActiveProfiles("test")
class FileUploadPropertiesTest {

	@Autowired
	private FileUploadProperties properties;

	@Autowired
	private Environment environment;

	@Test
	@DisplayName("파일 업로드 설정이 정상 바인딩된다")
	void properties_shouldBind() {
		assertThat(properties.getMaxSize()).isEqualTo(12345L);
		assertThat(properties.getMaxFiles()).isEqualTo(7);
		assertThat(properties.getAllowedContentTypes()).containsExactly("image/png", "application/pdf");
		assertThat(properties.getAllowedExtensions()).containsExactly("png", "pdf");
		assertThat(properties.isValidateSignature()).isTrue();
		assertThat(properties.getUploadDir()).isEqualTo("/tmp/uploads-test");
	}

	@Test
	@DisplayName("멀티파트 최대 크기 설정이 적용된다")
	void multipartProperties_shouldBind() {
		assertThat(environment.getProperty("spring.servlet.multipart.max-file-size")).isEqualTo("2MB");
		assertThat(environment.getProperty("spring.servlet.multipart.max-request-size")).isEqualTo("3MB");
	}
}
