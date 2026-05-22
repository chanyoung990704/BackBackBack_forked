package com.aivle.project.company.job;

import com.aivle.project.common.outbox.OutboxEvent;
import com.aivle.project.common.outbox.OutboxRepository;
import com.aivle.project.company.saga.AiReportSagaOrchestrator;
import com.aivle.project.company.saga.SagaInstance;
import com.aivle.project.company.saga.SagaInstanceRepository;
import com.aivle.project.company.saga.SagaStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * AI 비동기 작업을 제어하는 디스패처 서비스.
 * 기존 KafkaTemplate에 대한 직접 의존을 완전히 제거하고, 
 * 로컬 트랜잭션 범위 내에서 SagaInstance 생성 및 OutboxEvent 적재를 원자적(Atomic)으로 처리합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiJobDispatchService {

	@Value("${app.ai.job.request-topic:ai-job-request}")
	private String requestTopic;

	private final ObjectMapper objectMapper;
	private final SagaInstanceRepository sagaInstanceRepository;
	private final OutboxRepository outboxRepository;
	private final AiReportSagaOrchestrator aiReportSagaOrchestrator;

	/**
	 * 기업 리포트 분석 비동기 Saga 파이프라인을 개시합니다.
	 *
	 * @param requestId 요청 ID
	 * @param companyId 기업 ID
	 * @param year      연도
	 * @param quarter   분기
	 * @return Saga 개시 성공 여부 (로컬 트랜잭션 내에서 원자적으로 처리되어 즉시 응답 가능)
	 */
	@Transactional
	public boolean dispatchReport(String requestId, Long companyId, Integer year, Integer quarter) {
		log.info("Dispatching AI report via Saga & Outbox: requestId={}, companyId={}, year={}, quarter={}", 
				requestId, companyId, year, quarter);
		try {
			aiReportSagaOrchestrator.startSaga(requestId, companyId, year, quarter);
			return true;
		} catch (Exception e) {
			log.error("Failed to dispatch AI report saga for requestId: {}", requestId, e);
			return false;
		}
	}

	/**
	 * AI 종합 코멘트 웜업 비동기 작업을 Outbox를 통해 안전하게 적재 및 발행합니다.
	 *
	 * @param requestId 요청 ID
	 * @param companyId 기업 ID
	 * @param period    분기 정보
	 * @return 웜업 작업 적재 성공 여부 (즉시 응답 반환)
	 */
	@Transactional
	public boolean dispatchCommentWarmup(String requestId, Long companyId, String period) {
		log.info("Dispatching AI comment warmup via Saga & Outbox: requestId={}, companyId={}, period={}", 
				requestId, companyId, period);
		
		AiJobMessage message = AiJobMessage.forCommentWarmup(requestId, companyId, period);
		
		try {
			String payload = objectMapper.writeValueAsString(message);

			// 1. 웜업 상태를 관리할 SagaInstance 생성 및 저장 (STARTED)
			SagaInstance saga = SagaInstance.builder()
					.id(requestId)
					.sagaType("AI_COMMENT_WARMUP")
					.status(SagaStatus.STARTED)
					.currentStep("COMMENT_WARMUP")
					.payload(payload)
					.build();
			sagaInstanceRepository.save(saga);

			// 2. Outbox 테이블에 웜업 발행 이벤트 적재 (PENDING)
			OutboxEvent outboxEvent = OutboxEvent.builder()
					.eventType("AI_COMMENT_WARMUP_REQUEST")
					.aggregateType("SagaInstance")
					.aggregateId(requestId)
					.topic(requestTopic)
					.payload(payload)
					.build();
			outboxRepository.save(outboxEvent);

			log.info("Successfully registered AI comment warmup Saga & Outbox event: requestId={}", requestId);
			return true;
		} catch (JsonProcessingException e) {
			log.error("Failed to serialize AI comment warmup message: requestId={}", requestId, e);
			return false;
		} catch (Exception e) {
			log.error("Failed to dispatch AI comment warmup for requestId: {}", requestId, e);
			return false;
		}
	}
}
