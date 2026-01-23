package com.aivle.project.file.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 파일 저장소 설정.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.file.storage")
public class FileStorageProperties {

	private final S3 s3 = new S3();

	@Getter
	@Setter
	public static class S3 {
		private String bucket;
		private String region;

		private String prefix = "uploads";
	}
}
