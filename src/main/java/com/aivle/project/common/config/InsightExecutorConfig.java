package com.aivle.project.common.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 비동기 실행기 설정.
 */
@Configuration
@EnableAsync
@EnableConfigurationProperties(VirtualThreadProperties.class)
public class InsightExecutorConfig {

	private final VirtualThreadProperties virtualThreadProperties;

	public InsightExecutorConfig(VirtualThreadProperties virtualThreadProperties) {
		this.virtualThreadProperties = virtualThreadProperties;
	}

	@Bean(name = "insightExecutor")
	public Executor insightExecutor() {
		if (isInsightVirtualThreadEnabled()) {
			return newVirtualThreadExecutor("insight-vt-");
		}

		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(4);
		executor.setMaxPoolSize(8);
		executor.setQueueCapacity(100);
		executor.setThreadNamePrefix("insight-");
		executor.initialize();
		return executor;
	}

	@Bean(name = "emailExecutor")
	public Executor emailExecutor() {
		if (isEmailVirtualThreadEnabled()) {
			return newVirtualThreadExecutor("email-vt-");
		}

		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(4);
		executor.setQueueCapacity(100);
		executor.setThreadNamePrefix("email-");
		executor.initialize();
		return executor;
	}

	private boolean isInsightVirtualThreadEnabled() {
		return virtualThreadProperties.isEnabled() || virtualThreadProperties.isInsightEnabled();
	}

	private boolean isEmailVirtualThreadEnabled() {
		return virtualThreadProperties.isEnabled() || virtualThreadProperties.isEmailEnabled();
	}

	private ExecutorService newVirtualThreadExecutor(String namePrefix) {
		ThreadFactory factory = Thread.ofVirtual()
			.name(namePrefix, 1)
			.factory();
		return Executors.newThreadPerTaskExecutor(factory);
	}
}
