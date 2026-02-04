package com.aivle.project.company.client;

import com.aivle.project.company.dto.AiAnalysisResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.Disabled;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiServerClientTest {

    private MockWebServer mockWebServer;
    private AiServerClient aiServerClient;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        // 테스트용 Mock 서버 URL로 클라이언트 초기화
        String baseUrl = mockWebServer.url("/").toString();
        aiServerClient = new AiServerClient(baseUrl);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("AI 서버에서 예측 결과를 정상적으로 조회한다")
    void getPrediction_Success() {
        // given
        String mockResponseBody = """
            {
              "company_code": "[005930]",
              "company_name": "삼성전자",
              "base_period": "2025년 Q3",
              "predictions": {
                "ROA": 1.3982,
                "ROE": 1.4376,
                "부채비율": 30.64
              }
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseBody)
                .addHeader("Content-Type", "application/json"));

        // when
        AiAnalysisResponse response = aiServerClient.getPrediction("005930");

        // then
        assertThat(response.companyCode()).isEqualTo("[005930]");
        assertThat(response.companyName()).isEqualTo("삼성전자");
        assertThat(response.predictions()).containsEntry("ROA", 1.3982);
    }

    @Test
    @DisplayName("AI 서버 응답 실패 시 예외가 발생해야 한다")
    void getPrediction_Fail() {
        // given
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        // when & then
        assertThatThrownBy(() -> aiServerClient.getPrediction("005930"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("AI Server connection failed");
    }

    @Test
    @DisplayName("AI 서버에서 리포트 PDF를 정상적으로 다운로드한다")
    void getAnalysisReportPdf_Success() {
        // given
        byte[] mockPdfContent = "dummy-pdf-content".getBytes();
        mockWebServer.enqueue(new MockResponse()
                .setBody(new okio.Buffer().write(mockPdfContent))
                .addHeader("Content-Type", "application/pdf"));

        // when
        byte[] result = aiServerClient.getAnalysisReportPdf("005930");

        // then
        assertThat(result).isEqualTo(mockPdfContent);
    }

    @Test
    @Disabled("실제 외부 서버와 통신하는 테스트이므로 수동으로만 실행하세요.")
    @DisplayName("실제 AI 서버 연동 통합 테스트")
     void realServerIntegrationTest() {
        // given
        String realServerUrl = "https://bigbig-ai-server.azurewebsites.net";
        AiServerClient realClient = new AiServerClient(realServerUrl);
        String companyCode = "005930"; // 삼성전자

        // when
        AiAnalysisResponse response = realClient.getPrediction(companyCode);

        // then
        System.out.println("Real Server Response: " + response);
        assertThat(response).isNotNull();
        assertThat(response.companyName()).contains("삼성전자"); // 응답에 따라 조정 필요
        assertThat(response.predictions()).isNotEmpty();
    }
}
