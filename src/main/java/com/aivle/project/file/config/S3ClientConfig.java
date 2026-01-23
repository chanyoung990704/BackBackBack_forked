package com.aivle.project.file.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * S3 클라이언트 설정 (prod 전용).
 */
@Configuration
@Profile("prod")
public class S3ClientConfig {

	@Bean
	public S3Client s3Client(FileStorageProperties properties) {
		String bucket = properties.getS3().getBucket();
		String region = properties.getS3().getRegion();
		if (bucket == null || bucket.isBlank() || region == null || region.isBlank()) {
			throw new IllegalStateException("S3 설정(bucket/region)이 필요합니다.");
		}
		return S3Client.builder()
			.region(Region.of(region))
			.credentialsProvider(DefaultCredentialsProvider.create())
			.build();
	}
}
