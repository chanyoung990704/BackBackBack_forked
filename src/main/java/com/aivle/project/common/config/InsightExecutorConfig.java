package com.aivle.project.common.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 인사이트 조회 병렬 처리용 Executor 설정.
 */
@Configuration
@EnableAsync
public class InsightExecutorConfig {

	@Bean(name = "insightExecutor")
	public Executor insightExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(4);
		executor.setMaxPoolSize(8);
		executor.setQueueCapacity(100);
		executor.setThreadNamePrefix("insight-");
		executor.initialize();
		return executor;
	}
}
