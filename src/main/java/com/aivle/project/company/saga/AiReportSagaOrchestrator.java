package com.aivle.project.company.saga;

import com.aivle.project.common.outbox.OutboxEvent;
import com.aivle.project.common.outbox.OutboxRepository;
import com.aivle.project.company.job.AiJobMessage;
import com.aivle.project.company.job.AiJobType;
import com.aivle.project.company.service.AiReportRequestStatusService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * AI 리포트 생성 프로세스를 3단계(재무 수치 분석 -> 뉴스 감성 분석 -> 종합 코멘트 컴파일)로
 * 오케스트레이션하고, 장애 시 역순으로 보상 트랜잭션을 실행하는 Custom Saga Orchestrator.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiReportSagaOrchestrator {

    @Value("${app.ai.job.request-topic:ai-job-request}")
    private String requestTopic;

    @Value("${app.ai.job.rollback-topic:ai-job-rollback}")
    private String rollbackTopic;

    private final SagaInstanceRepository sagaInstanceRepository;
    private final OutboxRepository outboxRepository;
    private final AiReportRequestStatusService aiReportRequestStatusService;
    private final ObjectMapper objectMapper;

    /**
     * Saga 인스턴스를 생성하고 1단계(재무 수치 분석) 요청을 Outbox에 적재하여 전체 Saga 프로세스를 개시합니다.
     */
    @Transactional
    public void startSaga(String requestId, Long companyId, Integer year, Integer quarter) {
        log.info("Starting AI report generation Saga: requestId={}, companyId={}", requestId, companyId);
        
        AiJobMessage message = new AiJobMessage(
                requestId,
                AiJobType.AI_FINANCIAL_ANALYSIS,
                companyId,
                year,
                quarter,
                null,
                OffsetDateTime.now()
        );

        try {
            String payload = objectMapper.writeValueAsString(message);
            
            // 1. SagaInstance 상태 'STARTED' 저장
            SagaInstance saga = SagaInstance.builder()
                    .id(requestId)
                    .sagaType("AI_REPORT_GENERATION")
                    .status(SagaStatus.STARTED)
                    .currentStep("FINANCIAL_ANALYSIS")
                    .payload(payload)
                    .build();
            sagaInstanceRepository.save(saga);

            // 2. Outbox에 1단계 명령(재무 수치 분석) 적재
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventType("AI_REPORT_FINANCIAL_REQUEST")
                    .aggregateType("SagaInstance")
                    .aggregateId(requestId)
                    .topic(requestTopic)
                    .payload(payload)
                    .build();
            outboxRepository.save(outboxEvent);

            aiReportRequestStatusService.createPending(requestId, String.valueOf(companyId), year, quarter);
            log.info("Saga {} successfully initialized and financial analysis dispatched.", requestId);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize Saga message for start: requestId={}", requestId, e);
            throw new RuntimeException("Saga start failed due to serialization error", e);
        }
    }

    /**
     * 1단계(재무 수치 분석) 성공 시 호출되어 Saga 상태를 업데이트하고 2단계 뉴스 분석 명령을 Outbox에 적재합니다.
     */
    @Transactional
    public void onFinancialAnalysisSuccess(String requestId, String payload) {
        log.info("Saga {} Financial Analysis succeeded. Progressing to News Analysis.", requestId);

        SagaInstance saga = sagaInstanceRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Saga instance not found: " + requestId));

        try {
            AiJobMessage prevMessage = objectMapper.readValue(saga.getPayload(), AiJobMessage.class);
            AiJobMessage nextMessage = new AiJobMessage(
                    requestId,
                    AiJobType.AI_NEWS_ANALYSIS,
                    prevMessage.companyId(),
                    prevMessage.year(),
                    prevMessage.quarter(),
                    prevMessage.period(),
                    OffsetDateTime.now()
            );
            String nextPayload = objectMapper.writeValueAsString(nextMessage);

            // 1. SagaInstance 상태 업데이트
            saga.transitionTo(SagaStatus.FINANCIAL_ANALYZED, "NEWS_ANALYSIS");
            sagaInstanceRepository.save(saga);

            // 2. 2단계 뉴스 분석 명령을 Outbox에 적재
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventType("AI_REPORT_NEWS_REQUEST")
                    .aggregateType("SagaInstance")
                    .aggregateId(requestId)
                    .topic(requestTopic)
                    .payload(nextPayload)
                    .build();
            outboxRepository.save(outboxEvent);

            aiReportRequestStatusService.updateProcessing(requestId);
            log.info("Saga {} status updated to FINANCIAL_ANALYZED and news step dispatched.", requestId);
        } catch (JsonProcessingException e) {
            log.error("Serialization failed in onFinancialAnalysisSuccess: requestId={}", requestId, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 1단계(재무 수치 분석) 실패 시 호출되어 보상 없이 즉시 Saga를 최종 실패 처리합니다.
     */
    @Transactional
    public void onFinancialAnalysisFailure(String requestId, String reason) {
        log.warn("Saga {} Financial Analysis failed. Reason: {}. Transitioning directly to COMPENSATED (No rollback needed).", requestId, reason);

        SagaInstance saga = sagaInstanceRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Saga instance not found: " + requestId));

        saga.transitionTo(SagaStatus.COMPENSATED, "FAILED");
        sagaInstanceRepository.save(saga);

        aiReportRequestStatusService.updateFailed(requestId, "Financial analysis step failed: " + reason);
    }

    /**
     * 2단계(뉴스 감성 분석) 성공 시 호출되어 Saga 상태를 업데이트하고 3단계 종합 코멘트 컴파일 명령을 Outbox에 적재합니다.
     */
    @Transactional
    public void onNewsAnalysisSuccess(String requestId, String payload) {
        log.info("Saga {} News Analysis succeeded. Progressing to AI Comment Compilation.", requestId);

        SagaInstance saga = sagaInstanceRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Saga instance not found: " + requestId));

        try {
            AiJobMessage prevMessage = objectMapper.readValue(saga.getPayload(), AiJobMessage.class);
            AiJobMessage nextMessage = new AiJobMessage(
                    requestId,
                    AiJobType.AI_COMMENT_COMPILATION,
                    prevMessage.companyId(),
                    prevMessage.year(),
                    prevMessage.quarter(),
                    prevMessage.period(),
                    OffsetDateTime.now()
            );
            String nextPayload = objectMapper.writeValueAsString(nextMessage);

            // 1. SagaInstance 상태 업데이트
            saga.transitionTo(SagaStatus.NEWS_ANALYZED, "COMMENT_COMPILATION");
            sagaInstanceRepository.save(saga);

            // 2. 3단계 종합 코멘트 명령을 Outbox에 적재
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventType("AI_REPORT_COMMENT_REQUEST")
                    .aggregateType("SagaInstance")
                    .aggregateId(requestId)
                    .topic(requestTopic)
                    .payload(nextPayload)
                    .build();
            outboxRepository.save(outboxEvent);

            log.info("Saga {} status updated to NEWS_ANALYZED and comment step dispatched.", requestId);
        } catch (JsonProcessingException e) {
            log.error("Serialization failed in onNewsAnalysisSuccess: requestId={}", requestId, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 2단계(뉴스 감성 분석) 실패 시 호출되어 보상 트랜잭션(1단계 재무 데이터 삭제)을 개시합니다.
     */
    @Transactional
    public void onNewsAnalysisFailure(String requestId, String reason) {
        log.warn("Saga {} News Analysis failed. Reason: {}. Triggering compensating transactions.", requestId, reason);

        SagaInstance saga = sagaInstanceRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Saga instance not found: " + requestId));

        try {
            AiJobMessage prevMessage = objectMapper.readValue(saga.getPayload(), AiJobMessage.class);
            AiJobMessage rollbackMessage = new AiJobMessage(
                    requestId,
                    AiJobType.AI_FINANCIAL_COMPENSATE,
                    prevMessage.companyId(),
                    prevMessage.year(),
                    prevMessage.quarter(),
                    prevMessage.period(),
                    OffsetDateTime.now()
            );
            String rollbackPayload = objectMapper.writeValueAsString(rollbackMessage);

            // 1. SagaInstance 상태 COMPENSATING 업데이트
            saga.transitionTo(SagaStatus.COMPENSATING, "COMPENSATE_FINANCIAL");
            sagaInstanceRepository.save(saga);

            // 2. 1단계 재무 분석 보상(롤백) 명령을 Outbox에 적재
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventType("AI_REPORT_FINANCIAL_COMPENSATE")
                    .aggregateType("SagaInstance")
                    .aggregateId(requestId)
                    .topic(rollbackTopic)
                    .payload(rollbackPayload)
                    .build();
            outboxRepository.save(outboxEvent);

            log.warn("Saga {} news analysis failure compensation dispatched: delete financial data command.", requestId);
        } catch (JsonProcessingException e) {
            log.error("Serialization failed in onNewsAnalysisFailure: requestId={}", requestId, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 3단계(종합 코멘트 컴파일) 성공 시 호출되어 Saga 전체 프로세스를 AI_COMPLETED 상태로 완결 처리합니다.
     */
    @Transactional
    public void onCommentCompilationSuccess(String requestId, String fileId, String downloadUrl) {
        log.info("Saga {} AI Comment Compilation succeeded. Saga Completed!", requestId);

        SagaInstance saga = sagaInstanceRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Saga instance not found: " + requestId));

        // 1. SagaInstance 상태 AI_COMPLETED 업데이트
        saga.transitionTo(SagaStatus.AI_COMPLETED, "COMPLETED");
        sagaInstanceRepository.save(saga);

        // 2. 리포트 생성 최종 완료 처리
        aiReportRequestStatusService.updateCompleted(requestId, fileId, downloadUrl);
        log.info("Saga {} successfully completed.", requestId);
    }

    /**
     * 3단계(종합 코멘트 컴파일) 실패 시 호출되어 역순으로 보상 트랜잭션(2단계 뉴스 롤백)을 개시합니다.
     */
    @Transactional
    public void onCommentCompilationFailure(String requestId, String reason) {
        log.warn("Saga {} AI Comment Compilation failed. Reason: {}. Triggering compensation chain.", requestId, reason);

        SagaInstance saga = sagaInstanceRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Saga instance not found: " + requestId));

        try {
            AiJobMessage prevMessage = objectMapper.readValue(saga.getPayload(), AiJobMessage.class);
            AiJobMessage rollbackMessage = new AiJobMessage(
                    requestId,
                    AiJobType.AI_NEWS_COMPENSATE,
                    prevMessage.companyId(),
                    prevMessage.year(),
                    prevMessage.quarter(),
                    prevMessage.period(),
                    OffsetDateTime.now()
            );
            String rollbackPayload = objectMapper.writeValueAsString(rollbackMessage);

            // 1. SagaInstance 상태 COMPENSATING 업데이트
            saga.transitionTo(SagaStatus.COMPENSATING, "COMPENSATE_NEWS");
            sagaInstanceRepository.save(saga);

            // 2. 2단계 뉴스 롤백 보상 명령을 Outbox에 적재
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventType("AI_REPORT_NEWS_COMPENSATE")
                    .aggregateType("SagaInstance")
                    .aggregateId(requestId)
                    .topic(rollbackTopic)
                    .payload(rollbackPayload)
                    .build();
            outboxRepository.save(outboxEvent);

            log.warn("Saga {} comment compilation failure compensation chain started: dispatch news rollback.", requestId);
        } catch (JsonProcessingException e) {
            log.error("Serialization failed in onCommentCompilationFailure: requestId={}", requestId, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 2단계 뉴스 보상 트랜잭션 완료 시 호출되어 다음 보상인 1단계 재무 데이터 삭제 보상을 진행합니다.
     */
    @Transactional
    public void onNewsCompensated(String requestId) {
        log.info("Saga {} News Compensation completed. Progressing to Financial Compensation.", requestId);

        SagaInstance saga = sagaInstanceRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Saga instance not found: " + requestId));

        try {
            AiJobMessage prevMessage = objectMapper.readValue(saga.getPayload(), AiJobMessage.class);
            AiJobMessage rollbackMessage = new AiJobMessage(
                    requestId,
                    AiJobType.AI_FINANCIAL_COMPENSATE,
                    prevMessage.companyId(),
                    prevMessage.year(),
                    prevMessage.quarter(),
                    prevMessage.period(),
                    OffsetDateTime.now()
            );
            String rollbackPayload = objectMapper.writeValueAsString(rollbackMessage);

            // 1. SagaInstance 상태 COMPENSATING 유지하되 단계 수정
            saga.setCurrentStep("COMPENSATE_FINANCIAL");
            sagaInstanceRepository.save(saga);

            // 2. 1단계 재무 분석 보상(롤백) 명령을 Outbox에 적재
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventType("AI_REPORT_FINANCIAL_COMPENSATE")
                    .aggregateType("SagaInstance")
                    .aggregateId(requestId)
                    .topic(rollbackTopic)
                    .payload(rollbackPayload)
                    .build();
            outboxRepository.save(outboxEvent);

            log.info("Saga {} News compensated, Financial compensation dispatched.", requestId);
        } catch (JsonProcessingException e) {
            log.error("Serialization failed in onNewsCompensated: requestId={}", requestId, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 1단계 재무 보상 트랜잭션이 완료되었을 때 최종적으로 COMPENSATED(보상 완료 실패) 처리합니다.
     */
    @Transactional
    public void onFinancialCompensated(String requestId) {
        log.info("Saga {} Financial Compensation completed. Completing Saga Failure.", requestId);

        SagaInstance saga = sagaInstanceRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Saga instance not found: " + requestId));

        // 1. SagaInstance 상태 COMPENSATED 최종 실패 업데이트
        saga.transitionTo(SagaStatus.COMPENSATED, "FAILED");
        sagaInstanceRepository.save(saga);

        // 2. 리포트 생성 실패 처리
        aiReportRequestStatusService.updateFailed(requestId, "Saga execution failed and fully compensated.");
        log.info("Saga {} successfully compensated and closed.", requestId);
    }
}
