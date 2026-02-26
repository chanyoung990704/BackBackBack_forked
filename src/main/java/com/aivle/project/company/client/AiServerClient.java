package com.aivle.project.company.client;

import com.aivle.project.company.dto.AiAnalysisResponse;
import com.aivle.project.company.dto.AiCommentResponse;
import com.aivle.project.company.dto.AiHealthScoreResponse;
import com.aivle.project.company.dto.AiSignalResponse;
import com.aivle.project.common.error.ExternalAiUnavailableException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.util.UriBuilder;

@Slf4j
@Component
public class AiServerClient {

    private final WebClient webClient;
    private final boolean mockEnabled;
    private final long mockLatencyMs;
    private final Duration callTimeout;

    @Autowired
    public AiServerClient(
        @Qualifier("aiWebClient") WebClient aiWebClient,
        @Value("${ai.server.mock.enabled:false}") boolean mockEnabled,
        @Value("${ai.server.mock.latency-ms:0}") long mockLatencyMs,
        @Value("${ai.server.http.call-timeout-ms:10000}") long callTimeoutMs
    ) {
        this(aiWebClient, mockEnabled, mockLatencyMs, Duration.ofMillis(callTimeoutMs));
    }

    // 테스트 코드 호환을 위해 URL 기반 생성자를 유지한다.
    AiServerClient(String aiServerUrl, boolean mockEnabled, long mockLatencyMs) {
        this(WebClient.builder().baseUrl(aiServerUrl).build(), mockEnabled, mockLatencyMs, Duration.ofMillis(10000L));
    }

    // 타임아웃 테스트를 위한 생성자.
    AiServerClient(String aiServerUrl, boolean mockEnabled, long mockLatencyMs, long callTimeoutMs) {
        this(WebClient.builder().baseUrl(aiServerUrl).build(), mockEnabled, mockLatencyMs, Duration.ofMillis(callTimeoutMs));
    }

    private AiServerClient(WebClient webClient, boolean mockEnabled, long mockLatencyMs, Duration callTimeout) {
        this.webClient = webClient;
        this.mockEnabled = mockEnabled;
        this.mockLatencyMs = mockLatencyMs;
        this.callTimeout = callTimeout;
    }

    @CircuitBreaker(name = "aiServer")
    @Retry(name = "aiServer")
    @Bulkhead(name = "aiServer", type = Bulkhead.Type.SEMAPHORE)
    public AiAnalysisResponse getPrediction(String companyCode) {
        log.info("Requesting AI prediction for company: {}", companyCode);

        if (mockEnabled) {
            applyMockLatency();
            return mockPrediction(companyCode);
        }

        try {
            return getWithTimeout(
                builder -> builder.path("/api/v1/analysis/{companyCode}/predict").build(companyCode),
                AiAnalysisResponse.class
            );
        } catch (Exception e) {
            log.error("Failed to get prediction for company {}: {}", companyCode, e.getMessage());
            throw toExternalAiUnavailable(e);
        }
    }

    @CircuitBreaker(name = "aiServer")
    @Retry(name = "aiServer")
    @Bulkhead(name = "aiServer", type = Bulkhead.Type.SEMAPHORE)
    public byte[] getAnalysisReportPdf(String companyCode) {
        log.info("Downloading AI analysis report PDF for company: {}", companyCode);

        if (mockEnabled) {
            applyMockLatency();
            return mockPdfBytes(companyCode);
        }

        try {
            org.springframework.http.ResponseEntity<byte[]> response = webClient.get()
                .uri(builder -> builder.path("/api/v1/analysis/{companyCode}/report").build(companyCode))
                .retrieve()
                .toEntity(byte[].class)
                .timeout(callTimeout)
                .block();

            if (response == null || response.getBody() == null) {
                throw new RuntimeException("AI Server returned empty response");
            }

            org.springframework.http.MediaType contentType = response.getHeaders().getContentType();
            if (contentType == null || !contentType.equals(org.springframework.http.MediaType.APPLICATION_PDF)) {
                String bodyPreview = new String(response.getBody(), StandardCharsets.UTF_8);
                log.error("AI Server returned non-PDF response. Content-Type: {}, Body: {}", contentType, bodyPreview);
                throw new RuntimeException("AI Server returned invalid content type: " + contentType);
            }

            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to download report for company {}: {}", companyCode, e.getMessage());
            throw new RuntimeException("AI Server report download failed", e);
        }
    }

    @CircuitBreaker(name = "aiServer")
    @Retry(name = "aiServer")
    @Bulkhead(name = "aiServer", type = Bulkhead.Type.SEMAPHORE)
    public AiHealthScoreResponse getHealthScore(String companyCode) {
        log.info("Requesting AI health score for company: {}", companyCode);

        if (mockEnabled) {
            applyMockLatency();
            return mockHealthScore(companyCode);
        }

        try {
            return getWithTimeout(
                builder -> builder.path("/api/v1/analysis/{companyCode}/health-score").build(companyCode),
                AiHealthScoreResponse.class
            );
        } catch (Exception e) {
            log.error("Failed to get health score for company {}: {}", companyCode, e.getMessage());
            throw toExternalAiUnavailable(e);
        }
    }

    @CircuitBreaker(name = "aiServer")
    @Retry(name = "aiServer")
    @Bulkhead(name = "aiServer", type = Bulkhead.Type.SEMAPHORE)
    public AiSignalResponse getSignals(String companyCode, String period) {
        log.info("Requesting AI signals for company: {} (period: {})", companyCode, period);

        if (mockEnabled) {
            applyMockLatency();
            return mockSignals(companyCode, period);
        }

        try {
            return getWithTimeout(
                builder -> builder.path("/api/v1/analysis/{companyCode}/signals/{period}")
                    .build(companyCode, period),
                AiSignalResponse.class
            );
        } catch (Exception e) {
            log.error("Failed to get signals for company {}: {}", companyCode, e.getMessage());
            throw toExternalAiUnavailable(e);
        }
    }

    @CircuitBreaker(name = "aiServer")
    @Retry(name = "aiServer")
    @Bulkhead(name = "aiServer", type = Bulkhead.Type.SEMAPHORE)
    public AiCommentResponse getAiComment(String companyCode, String period) {
        log.info("Requesting AI comment for company: {} (period: {})", companyCode, period);

        if (mockEnabled) {
            applyMockLatency();
            return mockComment(companyCode, period);
        }

        try {
            return getWithTimeout(uriBuilder -> buildAiCommentUri(uriBuilder, companyCode, period), AiCommentResponse.class);
        } catch (Exception e) {
            log.error("Failed to get AI comment for company {}: {}", companyCode, e.getMessage());
            throw toExternalAiUnavailable(e);
        }
    }

    private java.net.URI buildAiCommentUri(UriBuilder uriBuilder, String companyCode, String period) {
        UriBuilder builder = uriBuilder.path("/api/v1/analysis/{companyCode}/ai-comment");
        if (period != null && !period.isBlank()) {
            builder.queryParam("period", period);
        }
        return builder.build(companyCode);
    }

    private <T> T getWithTimeout(java.util.function.Function<UriBuilder, java.net.URI> uriFunction, Class<T> responseType) {
        return webClient.get()
            .uri(uriFunction)
            .retrieve()
            .bodyToMono(responseType)
            .timeout(callTimeout)
            .block();
    }

    private ExternalAiUnavailableException toExternalAiUnavailable(Throwable throwable) {
        String reasonCode = resolveReasonCode(throwable);
        return new ExternalAiUnavailableException("AI Server connection failed", reasonCode, throwable);
    }

    private String resolveReasonCode(Throwable throwable) {
        if (containsCause(throwable, CallNotPermittedException.class)) {
            return "AI_CIRCUIT_OPEN";
        }
        if (containsCause(throwable, io.netty.handler.timeout.ReadTimeoutException.class)
            || containsCause(throwable, java.util.concurrent.TimeoutException.class)) {
            return "AI_TIMEOUT";
        }
        if (containsCause(throwable, WebClientRequestException.class)) {
            return "AI_UNAVAILABLE";
        }
        return "AI_UNAVAILABLE";
    }

    private boolean containsCause(Throwable throwable, Class<? extends Throwable> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
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
