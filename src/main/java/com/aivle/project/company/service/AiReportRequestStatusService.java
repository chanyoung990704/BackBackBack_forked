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

	private static final String UPDATE_STATUS_LUA =
		"local current = redis.call('get', KEYS[1])\n" +
		"if current then\n" +
		"    if string.find(current, '\"COMPLETED\"') or string.find(current, '\"FAILED\"') then\n" +
		"        if ARGV[2] == 'PENDING' or ARGV[2] == 'PROCESSING' then\n" +
		"            return 0\n" +
		"        end\n" +
		"    end\n" +
		"end\n" +
		"redis.call('setex', KEYS[1], ARGV[3], ARGV[1])\n" +
		"return 1";

	private void saveStatus(String requestId, AiReportStatusResponse status) {
		try {
			String json = objectMapper.writeValueAsString(status);
			org.springframework.data.redis.core.script.DefaultRedisScript<Long> redisScript = new org.springframework.data.redis.core.script.DefaultRedisScript<>(UPDATE_STATUS_LUA, Long.class);
			redisTemplate.execute(redisScript, java.util.Collections.singletonList(KEY_PREFIX + requestId), json, status.status(), String.valueOf(TTL.getSeconds()));
		} catch (JsonProcessingException e) {
			log.error("Failed to save status for request: {}", requestId, e);
		}
	}
}