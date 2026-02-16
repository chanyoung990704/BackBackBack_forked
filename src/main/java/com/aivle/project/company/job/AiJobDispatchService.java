package com.aivle.project.company.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * AI 장시간 작업 Kafka 디스패처.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiJobDispatchService {

	@Value("${app.ai.job.kafka-enabled:false}")
	private boolean kafkaEnabled;

	@Value("${app.ai.job.request-topic:ai-job-request}")
	private String requestTopic;

	private final ObjectMapper objectMapper;
	private final ObjectProvider<KafkaTemplate<String, String>> kafkaTemplateProvider;

	public boolean dispatchReport(String requestId, Long companyId, Integer year, Integer quarter) {
		return dispatch(AiJobMessage.forReport(requestId, companyId, year, quarter));
	}

	public boolean dispatchCommentWarmup(String requestId, Long companyId, String period) {
		return dispatch(AiJobMessage.forCommentWarmup(requestId, companyId, period));
	}

	private boolean dispatch(AiJobMessage message) {
		if (!kafkaEnabled) {
			return false;
		}
		KafkaTemplate<String, String> kafkaTemplate = kafkaTemplateProvider.getIfAvailable();
		if (kafkaTemplate == null) {
			log.warn("KafkaTemplate bean missing. Skip ai job dispatch: type={}, requestId={}", message.type(), message.requestId());
			return false;
		}
		try {
			String payload = objectMapper.writeValueAsString(message);
			kafkaTemplate.send(requestTopic, message.requestId(), payload);
			log.info("Dispatched AI job to Kafka: topic={}, type={}, requestId={}, companyId={}",
				requestTopic, message.type(), message.requestId(), message.companyId());
			return true;
		} catch (JsonProcessingException e) {
			log.error("Failed to serialize AI job message: type={}, requestId={}", message.type(), message.requestId(), e);
			return false;
		}
	}
}
