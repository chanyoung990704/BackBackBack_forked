package com.aivle.project.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Virtual Thread 실행기 설정.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.virtual-thread")
public class VirtualThreadProperties {

	/**
	 * 전체 Virtual Thread 기능 토글.
	 */
	private boolean enabled;

	/**
	 * 인사이트 비동기 실행기 Virtual Thread 토글.
	 */
	private boolean insightEnabled;

	/**
	 * 이메일 비동기 실행기 Virtual Thread 토글.
	 */
	private boolean emailEnabled;
}
