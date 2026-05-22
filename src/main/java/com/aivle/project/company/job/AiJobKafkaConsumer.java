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

	private void process(AiJobMessage message, String payload) {
		log.info("Processing AI job message type: {}, requestId: {}", message.type(), message.requestId());
		try {
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
				case AI_FINANCIAL_ANALYSIS -> {
					try {
						companyAiService.getCompanyAnalysis(message.companyId(), message.year(), message.quarter());
						aiReportSagaOrchestrator.onFinancialAnalysisSuccess(message.requestId(), payload);
					} catch (Exception e) {
						log.error("Failed in AI_FINANCIAL_ANALYSIS step for requestId: {}", message.requestId(), e);
						aiReportSagaOrchestrator.onFinancialAnalysisFailure(message.requestId(), e.getMessage());
					}
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
					try {
						FilesEntity file = companyAiService.generateAndSaveReport(message.companyId(), message.year(), message.quarter());
						String downloadUrl = "/api/companies/" + message.companyId() + "/ai-report/download?year=" + message.year() + "&quarter=" + message.quarter();
						aiReportSagaOrchestrator.onCommentCompilationSuccess(message.requestId(), String.valueOf(file.getId()), downloadUrl);
					} catch (Exception e) {
						log.error("Failed in AI_COMMENT_COMPILATION step for requestId: {}", message.requestId(), e);
						aiReportSagaOrchestrator.onCommentCompilationFailure(message.requestId(), e.getMessage());
					}
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
}
