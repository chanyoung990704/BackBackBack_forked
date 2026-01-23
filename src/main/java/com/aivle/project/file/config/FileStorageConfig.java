package com.aivle.project.file.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 파일 저장소 설정 바인딩.
 */
@Configuration
@EnableConfigurationProperties({FileStorageProperties.class, FileUploadProperties.class})
public class FileStorageConfig {
}
