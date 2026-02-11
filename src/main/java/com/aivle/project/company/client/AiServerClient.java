package com.aivle.project.company.client;

import com.aivle.project.company.dto.AiAnalysisResponse;
import com.aivle.project.company.dto.AiCommentResponse;
import com.aivle.project.company.dto.AiHealthScoreResponse;
import com.aivle.project.company.dto.AiSignalResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;

@Slf4j
@Component
public class AiServerClient {

    private final WebClient webClient;
    private final boolean mockEnabled;
    private final long mockLatencyMs;

    public AiServerClient(
        @Value("${ai.server.url}") String aiServerUrl,
        @Value("${ai.server.mock.enabled:false}") boolean mockEnabled,
        @Value("${ai.server.mock.latency-ms:0}") long mockLatencyMs
    ) {
        this.webClient = WebClient.builder()
                .baseUrl(aiServerUrl)
                .build();
        this.mockEnabled = mockEnabled;
        this.mockLatencyMs = mockLatencyMs;
    }

    public AiAnalysisResponse getPrediction(String companyCode) {
        log.info("Requesting AI prediction for company: {}", companyCode);

        if (mockEnabled) {
            applyMockLatency();
            return mockPrediction(companyCode);
        }

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

        if (mockEnabled) {
            applyMockLatency();
            return mockPdfBytes(companyCode);
        }

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

        if (mockEnabled) {
            applyMockLatency();
            return mockHealthScore(companyCode);
        }

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

        if (mockEnabled) {
            applyMockLatency();
            return mockSignals(companyCode, period);
        }

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

        if (mockEnabled) {
            applyMockLatency();
            return mockComment(companyCode, period);
        }

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

    private void applyMockLatency() {
        if (mockLatencyMs <= 0) {
            return;
        }
        try {
            Thread.sleep(mockLatencyMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private AiAnalysisResponse mockPrediction(String companyCode) {
        String basePeriod = calculateBasePeriod();
        return new AiAnalysisResponse(
            companyCode,
            "PERF_MOCK_COMPANY",
            basePeriod,
            Map.of(
                "ROA", 4.2,
                "ROE", 7.8,
                "DEBT_RATIO", 120.5
            )
        );
    }

    private byte[] mockPdfBytes(String companyCode) {
        String payload = "PERF_MOCK_PDF_" + companyCode;
        return payload.getBytes(StandardCharsets.UTF_8);
    }

    private AiHealthScoreResponse mockHealthScore(String companyCode) {
        String currentPeriod = calculateBasePeriod();
        return new AiHealthScoreResponse(
            companyCode,
            "PERF_MOCK_COMPANY",
            java.util.List.of(
                new AiHealthScoreResponse.HealthScoreQuarter(currentPeriod, 73.0, "주의", "ACTUAL")
            ),
            73,
            77
        );
    }

    private AiSignalResponse mockSignals(String companyCode, String period) {
        return new AiSignalResponse(
            companyCode,
            "PERF_MOCK_COMPANY",
            "PERF_INDUSTRY",
            period,
            Map.of(
                "ROA", "GREEN",
                "ROE", "YELLOW",
                "DEBT_RATIO", "RED"
            )
        );
    }

    private AiCommentResponse mockComment(String companyCode, String period) {
        return new AiCommentResponse(
            companyCode,
            "PERF_MOCK_COMPANY",
            "PERF_INDUSTRY",
            period,
            "모의 AI 코멘트입니다."
        );
    }

    private String calculateBasePeriod() {
        LocalDate now = LocalDate.now();
        int quarter = ((now.getMonthValue() - 1) / 3) + 1;
        return now.getYear() + String.valueOf(quarter);
    }
}
