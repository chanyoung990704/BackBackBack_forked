package com.aivle.project.company.client;

import com.aivle.project.company.dto.AiAnalysisResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

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
}
