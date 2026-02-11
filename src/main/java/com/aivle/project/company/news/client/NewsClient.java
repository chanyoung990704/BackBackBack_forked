package com.aivle.project.company.news.client;

import com.aivle.project.company.news.dto.NewsApiResponse;
import com.aivle.project.company.news.dto.NewsItemResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.codec.DecodingException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI 서버 뉴스 분석 API 클라이언트.
 */
@Slf4j
@Component
public class NewsClient {

    private final WebClient webClient;
    private final boolean mockEnabled;
    private final long mockLatencyMs;

    public NewsClient(
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

    /**
     * AI 서버에서 뉴스 분석 데이터를 가져옵니다.
     *
     * @param companyCode 기업 코드 (stock_code)
     * @param companyName 기업명
     * @return 뉴스 분석 응답
     */
	public NewsApiResponse fetchNews(String companyCode, String companyName) {
		log.info("Requesting news for company: {} ({})", companyCode, companyName);

		if (mockEnabled) {
			applyMockLatency();
			return mockNews(companyCode, companyName);
		}

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

	/**
	 * AI 서버에서 사업보고서 분석 데이터를 가져옵니다.
	 *
	 * @param companyCode 기업 코드 (stock_code)
	 * @return 사업보고서 분석 응답
	 */
	public com.aivle.project.company.reportanalysis.dto.ReportApiResponse fetchReport(String companyCode) {
		log.info("Requesting report analysis for company: {}", companyCode);

		if (mockEnabled) {
			applyMockLatency();
			return mockReport(companyCode);
		}

		try {
			return webClient.get()
				.uri("/api/v1/news/{companyCode}/report", companyCode)
				.retrieve()
				.bodyToMono(com.aivle.project.company.reportanalysis.dto.ReportApiResponse.class)
				.block();
		} catch (DecodingException e) {
			log.error("Failed to decode AI report response for company {}: {}", companyCode, e.getMessage());
			throw new RuntimeException("AI Server response format error: invalid datetime field", e);
		} catch (Exception e) {
			log.error("Failed to fetch report analysis for company {}: {}", companyCode, e.getMessage());
			throw new RuntimeException("AI Server connection failed", e);
		}
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

	private NewsApiResponse mockNews(String companyCode, String companyName) {
		LocalDateTime now = LocalDateTime.now();
		List<NewsItemResponse> items = List.of(
			new NewsItemResponse("모의 뉴스 1", "요약 1", 0.71, now.minusMinutes(2).toString(), "https://example.com/news/1", "POS"),
			new NewsItemResponse("모의 뉴스 2", "요약 2", 0.22, now.minusMinutes(5).toString(), "https://example.com/news/2", "NEU"),
			new NewsItemResponse("모의 뉴스 3", "요약 3", -0.12, now.minusMinutes(8).toString(), "https://example.com/news/3", "NEG")
		);
		return new NewsApiResponse(
			companyName == null ? "PERF_MOCK_COMPANY" : companyName,
			items.size(),
			items,
			0.27,
			now.toString()
		);
	}

	private com.aivle.project.company.reportanalysis.dto.ReportApiResponse mockReport(String companyCode) {
		LocalDateTime now = LocalDateTime.now();
		List<NewsItemResponse> items = List.of(
			new NewsItemResponse("모의 리포트 1", "리포트 요약 1", 0.61, now.minusMinutes(3).toString(), "https://example.com/report/1", "POS"),
			new NewsItemResponse("모의 리포트 2", "리포트 요약 2", 0.14, now.minusMinutes(7).toString(), "https://example.com/report/2", "NEU")
		);
		return new com.aivle.project.company.reportanalysis.dto.ReportApiResponse(
			"PERF_MOCK_COMPANY_" + companyCode,
			items.size(),
			items,
			0.38,
			now.toString()
		);
	}
}
