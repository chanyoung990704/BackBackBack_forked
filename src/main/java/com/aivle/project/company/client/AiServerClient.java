package com.aivle.project.company.client;

import com.aivle.project.company.dto.AiAnalysisResponse;
import com.aivle.project.company.dto.AiCommentResponse;
import com.aivle.project.company.dto.AiHealthScoreResponse;
import com.aivle.project.company.dto.AiSignalResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;

@Slf4j
@Component
public class AiServerClient {

    private final WebClient webClient;

    public AiServerClient(@Value("${ai.server.url}") String aiServerUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(aiServerUrl)
                .build();
    }

    public AiAnalysisResponse getPrediction(String companyCode) {
        log.info("Requesting AI prediction for company: {}", companyCode);

        try {
            return webClient.get()
                    .uri("/api/v1/analysis/{companyCode}/predict", companyCode)
                    .retrieve()
                    .bodyToMono(AiAnalysisResponse.class)
                    .block();
        } catch (Exception e) {
            log.error("Failed to get prediction for company {}: {}", companyCode, e.getMessage());
            throw new RuntimeException("AI Server connection failed", e);
        }
    }

    public byte[] getAnalysisReportPdf(String companyCode) {
        log.info("Downloading AI analysis report PDF for company: {}", companyCode);

        try {
            return webClient.get()
                    .uri("/api/v1/analysis/{companyCode}/report", companyCode)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();
        } catch (Exception e) {
            log.error("Failed to download report for company {}: {}", companyCode, e.getMessage());
            throw new RuntimeException("AI Server report download failed", e);
        }
    }

    public AiHealthScoreResponse getHealthScore(String companyCode) {
        log.info("Requesting AI health score for company: {}", companyCode);

        try {
            return webClient.get()
                    .uri("/api/v1/analysis/{companyCode}/health-score", companyCode)
                    .retrieve()
                    .bodyToMono(AiHealthScoreResponse.class)
                    .block();
        } catch (Exception e) {
            log.error("Failed to get health score for company {}: {}", companyCode, e.getMessage());
            throw new RuntimeException("AI Server connection failed", e);
        }
    }

    public AiSignalResponse getSignals(String companyCode, String period) {
        log.info("Requesting AI signals for company: {} (period: {})", companyCode, period);

        try {
            return webClient.get()
                    .uri("/api/v1/analysis/{companyCode}/signals/{period}", companyCode, period)
                    .retrieve()
                    .bodyToMono(AiSignalResponse.class)
                    .block();
        } catch (Exception e) {
            log.error("Failed to get signals for company {}: {}", companyCode, e.getMessage());
            throw new RuntimeException("AI Server connection failed", e);
        }
    }

    public AiCommentResponse getAiComment(String companyCode, String period) {
        log.info("Requesting AI comment for company: {} (period: {})", companyCode, period);

        try {
            return webClient.get()
                .uri(uriBuilder -> buildAiCommentUri(uriBuilder, companyCode, period))
                .retrieve()
                .bodyToMono(AiCommentResponse.class)
                .block();
        } catch (Exception e) {
            log.error("Failed to get AI comment for company {}: {}", companyCode, e.getMessage());
            throw new RuntimeException("AI Server connection failed", e);
        }
    }

    private java.net.URI buildAiCommentUri(UriBuilder uriBuilder, String companyCode, String period) {
        UriBuilder builder = uriBuilder.path("/api/v1/analysis/{companyCode}/ai-comment");
        if (period != null && !period.isBlank()) {
            builder.queryParam("period", period);
        }
        return builder.build(companyCode);
    }
}
