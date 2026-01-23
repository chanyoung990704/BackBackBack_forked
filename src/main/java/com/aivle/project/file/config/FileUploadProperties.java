package com.aivle.project.file.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 파일 업로드 제한 설정.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.file.upload")
public class FileUploadProperties {

	@Positive
	private long maxSize = 10 * 1024 * 1024L;

	@Positive
	private int maxFiles = 5;

	private List<String> allowedContentTypes = new ArrayList<>();

	private List<String> allowedExtensions = new ArrayList<>();

	private boolean validateSignature = false;

	@NotBlank
	private String uploadDir = System.getProperty("user.home") + "/uploads";
}
