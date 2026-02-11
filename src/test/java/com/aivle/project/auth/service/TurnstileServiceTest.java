package com.aivle.project.auth.service;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class TurnstileServiceTest {

    private MockWebServer mockWebServer;
    private TurnstileService turnstileService;

    @BeforeEach
    void setUp() {
        mockWebServer = new MockWebServer();

        WebClient webClient = WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(
                HttpClient.create().baseUrl(mockWebServer.url("/").toString())
            ))
            .build();

        turnstileService = new TurnstileService(webClient);
        ReflectionTestUtils.setField(turnstileService, "secretKey", "test-secret-key");
        ReflectionTestUtils.setField(turnstileService, "verifyUrl", mockWebServer.url("/").toString());
        ReflectionTestUtils.setField(turnstileService, "timeoutMs", 5000);
        ReflectionTestUtils.setField(turnstileService, "mockAllowAll", false);
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    void shouldReturnTrueWhenTurnstileVerificationSucceeds() {
        // Given
        String successResponse = """
            {
                "success": true,
                "challenge_ts": "2024-01-27T10:00:00.000Z",
                "hostname": "example.com"
            }
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(successResponse)
            .addHeader("Content-Type", "application/json"));

        // When & Then
        StepVerifier.create(turnstileService.verifyToken("valid-token", "192.168.1.1"))
            .expectNext(true)
            .verifyComplete();
    }

    @Test
    void shouldReturnFalseWhenTurnstileVerificationFails() {
        // Given
        String failureResponse = """
            {
                "success": false,
                "error-codes": ["invalid-input-response"]
            }
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(failureResponse)
            .addHeader("Content-Type", "application/json"));

        // When & Then
        StepVerifier.create(turnstileService.verifyToken("invalid-token", "192.168.1.1"))
            .expectNext(false)
            .verifyComplete();
    }

    @Test
    void shouldReturnFalseWhenTokenIsEmpty() {
        // When & Then
        StepVerifier.create(turnstileService.verifyToken("", null))
            .expectNext(false)
            .verifyComplete();
    }

    @Test
    void shouldReturnFalseWhenTokenIsNull() {
        // When & Then
        StepVerifier.create(turnstileService.verifyToken(null, null))
            .expectNext(false)
            .verifyComplete();
    }

    @Test
    void shouldReturnFalseWhenSecretKeyIsNotConfigured() {
        // Given
        ReflectionTestUtils.setField(turnstileService, "secretKey", "");

        // When & Then
        StepVerifier.create(turnstileService.verifyToken("some-token", null))
            .expectNext(false)
            .verifyComplete();
    }

    @Test
    void shouldReturnFalseWhenApiCallFails() {
        // Given
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        // When & Then
        StepVerifier.create(turnstileService.verifyToken("some-token", null))
            .expectNext(false)
            .verifyComplete();
    }

    @Test
    void shouldReturnTrueWhenMockAllowAllEnabled() {
        // Given
        ReflectionTestUtils.setField(turnstileService, "mockAllowAll", true);

        // When & Then
        StepVerifier.create(turnstileService.verifyToken("", null))
            .expectNext(true)
            .verifyComplete();
    }

    @Test
    void shouldVerifyTokenSyncSuccessfully() {
        // Given
        String successResponse = """
            {
                "success": true,
                "challenge_ts": "2024-01-27T10:00:00.000Z"
            }
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(successResponse)
            .addHeader("Content-Type", "application/json"));

        // When
        boolean result = turnstileService.verifyTokenSync("valid-token", null);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void shouldVerifyTokenSyncWithFailure() {
        // Given
        String failureResponse = """
            {
                "success": false,
                "error-codes": ["timeout-or-duplicate"]
            }
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(failureResponse)
            .addHeader("Content-Type", "application/json"));

        // When
        boolean result = turnstileService.verifyTokenSync("invalid-token", null);

        // Then
        assertThat(result).isFalse();
    }
}
