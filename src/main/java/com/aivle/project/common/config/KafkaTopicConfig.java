package com.aivle.project.common.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * AI 작업 토픽 생성 설정.
 */
@Configuration
public class KafkaTopicConfig {

	@Bean
	public NewTopic aiJobRequestTopic(@Value("${app.ai.job.request-topic:ai-job-request}") String requestTopic) {
		return TopicBuilder.name(requestTopic)
			.partitions(3)
			.replicas(1)
			.build();
	}
}
