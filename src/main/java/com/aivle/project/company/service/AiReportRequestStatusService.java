package com.aivle.project.company.service;

import com.aivle.project.company.dto.AiReportStatusResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * AI 리포트 요청 상태 관리 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiReportRequestStatusService {

	private static final String KEY_PREFIX = "ai:report:request:";
	private static final Duration TTL = Duration.ofHours(24);

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	public void createPending(String requestId, String companyCode, Integer year, Integer quarter) {
		AiReportStatusResponse status = AiReportStatusResponse.pending(requestId);
		saveStatus(requestId, status);
		log.info("Created pending request: {} for company: {} ({}/{})", requestId, companyCode, year, quarter);
	}

	public void updateProcessing(String requestId) {
		AiReportStatusResponse status = AiReportStatusResponse.processing(requestId);
		saveStatus(requestId, status);
		log.info("Request {} processing", requestId);
	}

	public void updateCompleted(String requestId, String fileId, String downloadUrl) {
		AiReportStatusResponse status = AiReportStatusResponse.completed(requestId, fileId, downloadUrl);
		saveStatus(requestId, status);
		log.info("Request {} completed: fileId={}", requestId, fileId);
	}

	public void updateFailed(String requestId, String errorMessage) {
		AiReportStatusResponse status = AiReportStatusResponse.failed(requestId, errorMessage);
		saveStatus(requestId, status);
		log.warn("Request {} failed: {}", requestId, errorMessage);
	}

	public Optional<AiReportStatusResponse> getStatus(String requestId) {
		String json = redisTemplate.opsForValue().get(KEY_PREFIX + requestId);
		if (json == null) {
			return Optional.empty();
		}
		try {
			return Optional.of(objectMapper.readValue(json, AiReportStatusResponse.class));
		} catch (JsonProcessingException e) {
			log.error("Failed to parse status for request: {}", requestId, e);
			return Optional.empty();
		}
	}

	private void saveStatus(String requestId, AiReportStatusResponse status) {
		try {
			String json = objectMapper.writeValueAsString(status);
			redisTemplate.opsForValue().set(KEY_PREFIX + requestId, json, TTL);
		} catch (JsonProcessingException e) {
			log.error("Failed to save status for request: {}", requestId, e);
		}
	}
}