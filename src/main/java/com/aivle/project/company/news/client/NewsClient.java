package com.aivle.project.company.news.client;

import com.aivle.project.company.news.dto.NewsApiResponse;
import com.aivle.project.company.news.dto.NewsItemResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.codec.DecodingException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * AI 서버 뉴스 분석 API 클라이언트.
 */
@Slf4j
@Component
public class NewsClient {

    private final WebClient webClient;

    public NewsClient(@Value("${ai.server.url}") String aiServerUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(aiServerUrl)
                .build();
    }

    /**
     * AI 서버에서 뉴스 분석 데이터를 가져옵니다.
     *
     * @param companyCode 기업 코드 (stock_code)
     * @param companyName 기업명
     * @return 뉴스 분석 응답
     */
    public NewsApiResponse fetchNews(String companyCode, String companyName) {
        log.info("Requesting news for company: {} ({})", companyCode, companyName);

        try {
            return webClient.post()
                    .uri("/api/v1/news/{companyCode}", companyCode)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("company_name", companyName))
                    .retrieve()
                    .bodyToMono(NewsApiResponse.class)
                    .block();
        } catch (DecodingException e) {
            log.error("Failed to decode AI news response for company {}: {}", companyCode, e.getMessage());
            throw new RuntimeException("AI Server response format error: invalid datetime field", e);
        } catch (Exception e) {
            log.error("Failed to fetch news for company {}: {}", companyCode, e.getMessage());
            throw new RuntimeException("AI Server connection failed", e);
        }
    }
}
