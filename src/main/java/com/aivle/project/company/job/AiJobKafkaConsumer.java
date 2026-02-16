package com.aivle.project.company.job;

import com.aivle.project.company.service.CompanyAiCommentService;
import com.aivle.project.company.service.CompanyAiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * AI 작업 Kafka 컨슈머.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiJobKafkaConsumer {

	private final ObjectMapper objectMapper;
	private final CompanyAiService companyAiService;
	private final CompanyAiCommentService companyAiCommentService;
	@Qualifier("insightExecutor")
	private final Executor insightExecutor;

	@KafkaListener(
		topics = "${app.ai.job.request-topic:ai-job-request}",
		groupId = "${APP_AI_JOB_KAFKA_GROUP_ID:ai-job-consumer}"
	)
	public void consume(String payload) {
		try {
			AiJobMessage message = objectMapper.readValue(payload, AiJobMessage.class);
			CompletableFuture.runAsync(() -> process(message), insightExecutor)
				.exceptionally(throwable -> {
					log.error("AI job async execution failed: requestId={}, type={}",
						message.requestId(), message.type(), throwable);
					return null;
				});
		} catch (Exception e) {
			log.error("Failed to consume AI job payload: {}", payload, e);
		}
	}

	private void process(AiJobMessage message) {
		switch (message.type()) {
			case AI_REPORT -> companyAiService.processReportGeneration(
				message.requestId(),
				message.companyId(),
				message.year(),
				message.quarter()
			);
			case AI_COMMENT_WARMUP -> companyAiCommentService.ensureAiCommentCached(
				message.companyId(),
				message.period()
			);
			default -> log.warn("Unsupported AI job type: {}", message.type());
		}
	}
}
