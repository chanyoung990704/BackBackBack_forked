package com.aivle.project.company.job;

import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.repository.CompaniesRepository;
import com.aivle.project.company.saga.AiReportSagaOrchestrator;
import com.aivle.project.company.service.CompanyAiCommentService;
import com.aivle.project.company.service.CompanyAiService;
import com.aivle.project.company.service.CompanyReputationScoreService;
import com.aivle.project.file.entity.FilesEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
	private final AiReportSagaOrchestrator aiReportSagaOrchestrator;
	private final CompanyReputationScoreService companyReputationScoreService;
	private final CompaniesRepository companiesRepository;
	private final CompanyAiReportStoreService companyAiReportStoreService;

	@KafkaListener(
		topics = {"${app.ai.job.request-topic:ai-job-request}", "${app.ai.job.rollback-topic:ai-job-rollback}"},
		groupId = "${APP_AI_JOB_KAFKA_GROUP_ID:ai-job-consumer}"
	)
	public void consume(String payload) {
		try {
			AiJobMessage message = objectMapper.readValue(payload, AiJobMessage.class);
			process(message, payload);
		} catch (Exception e) {
			log.error("Failed to consume AI job payload: {}", payload, e);
			throw new RuntimeException("Failed to process AI job", e);
		}
	}

	@KafkaListener(
		topics = {"${app.ai.job.response-topic:ai-job-response}"},
		groupId = "${APP_AI_JOB_KAFKA_GROUP_ID:ai-job-consumer}-response"
	)
	public void consumeResponse(String payload) {
		log.info("Received AI job response payload: {}", payload);
		try {
			AiJobResponseMessage message = objectMapper.readValue(payload, AiJobResponseMessage.class);
			processResponse(message, payload);
		} catch (Exception e) {
			log.error("Failed to consume AI job response payload: {}", payload, e);
		}
	}

	private void process(AiJobMessage message, String payload) {
		log.info("Processing AI job message type: {}, requestId: {}", message.type(), message.requestId());
		try {
			switch (message.type()) {
				case AI_REPORT -> {
					// 기존 직접 생성 로직도 Feature Toggle 형태의 로깅으로 정리
					log.info("AI_REPORT requested. Delegated to Python worker async via Kafka pipeline. Skipping Java synchronous path.");
				}
				case AI_COMMENT_WARMUP -> companyAiCommentService.ensureAiCommentCached(
					message.companyId(),
					message.period()
				);
				case AI_FINANCIAL_ANALYSIS -> {
					// 1단계 분석 요청은 Python Worker가 구독/처리하므로 Java 웹서버 Consumer는 스킵합니다.
					log.info("Saga FINANCIAL_ANALYSIS step requested. Handled by Python worker. Java Consumer skipping request.");
				}
				case AI_NEWS_ANALYSIS -> {
					try {
						CompaniesEntity company = companiesRepository.findById(message.companyId())
							.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기업 ID입니다: " + message.companyId()));
						companyReputationScoreService.syncExternalHealthScoreIfPresent(message.companyId(), company.getStockCode());
						aiReportSagaOrchestrator.onNewsAnalysisSuccess(message.requestId(), payload);
					} catch (Exception e) {
						log.error("Failed in AI_NEWS_ANALYSIS step for requestId: {}", message.requestId(), e);
						aiReportSagaOrchestrator.onNewsAnalysisFailure(message.requestId(), e.getMessage());
					}
				}
				case AI_COMMENT_COMPILATION -> {
					// 3단계 컴파일 요청은 Python Worker가 구독/처리하므로 Java 웹서버 Consumer는 스킵합니다.
					log.info("Saga COMMENT_COMPILATION step requested. Handled by Python worker. Java Consumer skipping request.");
				}
				case AI_FINANCIAL_COMPENSATE -> {
					try {
						companyAiService.rollbackFinancialAnalysis(message.companyId(), message.year(), message.quarter());
						aiReportSagaOrchestrator.onFinancialCompensated(message.requestId());
					} catch (Exception e) {
						log.error("Failed to rollback Financial Analysis for requestId: {}", message.requestId(), e);
					}
				}
				case AI_NEWS_COMPENSATE -> {
					try {
						companyAiService.rollbackNewsAnalysis(message.companyId());
						aiReportSagaOrchestrator.onNewsCompensated(message.requestId());
					} catch (Exception e) {
						log.error("Failed to rollback News Analysis for requestId: {}", message.requestId(), e);
					}
				}
				default -> log.warn("Unsupported AI job type: {}", message.type());
			}
		} catch (Exception e) {
			log.error("Failed processing AI job message type: {} for requestId: {}", message.type(), message.requestId(), e);
		}
	}

	private void processResponse(AiJobResponseMessage message, String payload) {
		log.info("Processing AI job response for requestId: {}, type: {}, success: {}", 
			message.requestId(), message.type(), message.isSuccess());
		
		try {
			if (!message.isSuccess()) {
				log.warn("AI job failed on worker side for requestId: {}. Error: {}", message.requestId(), message.errorMessage());
				if (message.type() == AiJobType.AI_FINANCIAL_ANALYSIS) {
					aiReportSagaOrchestrator.onFinancialAnalysisFailure(message.requestId(), message.errorMessage());
				} else if (message.type() == AiJobType.AI_COMMENT_COMPILATION) {
					aiReportSagaOrchestrator.onCommentCompilationFailure(message.requestId(), message.errorMessage());
				}
				return;
			}

			switch (message.type()) {
				case AI_FINANCIAL_ANALYSIS -> {
					// Python Worker가 구한 예측 수치 저장
					companyAiService.savePythonPredictions(
						message.companyId(),
						message.year(),
						message.quarter(),
						message.predictions()
					);
					// Saga 성공 처리 연동
					aiReportSagaOrchestrator.onFinancialAnalysisSuccess(message.requestId(), payload);
					log.info("Successfully handled Python AI_FINANCIAL_ANALYSIS response for requestId: {}", message.requestId());
				}
				case AI_COMMENT_COMPILATION -> {
					// Python Worker가 저장 완료한 스토리지 키 바인딩
					FilesEntity file = companyAiReportStoreService.linkSavedReport(
						message.companyId(),
						message.year(),
						message.quarter(),
						message.storageKey(),
						message.filename()
					);
					String downloadUrl = "/api/companies/" + message.companyId() + "/ai-report/download?year=" + message.year() + "&quarter=" + message.quarter();
					// Saga 최종 완결 처리 연동
					aiReportSagaOrchestrator.onCommentCompilationSuccess(
						message.requestId(),
						String.valueOf(file.getId()),
						downloadUrl
					);
					log.info("Successfully handled Python AI_COMMENT_COMPILATION response for requestId: {}", message.requestId());
				}
				default -> log.warn("Unsupported AI job response type: {}", message.type());
			}
		} catch (Exception e) {
			log.error("Failed processing AI job response for requestId: {}", message.requestId(), e);
			// 런타임 에러 시 Saga를 실패 처리하여 데드락 방지
			if (message.type() == AiJobType.AI_FINANCIAL_ANALYSIS) {
				aiReportSagaOrchestrator.onFinancialAnalysisFailure(message.requestId(), e.getMessage());
			} else if (message.type() == AiJobType.AI_COMMENT_COMPILATION) {
				aiReportSagaOrchestrator.onCommentCompilationFailure(message.requestId(), e.getMessage());
			}
		}
	}
}
