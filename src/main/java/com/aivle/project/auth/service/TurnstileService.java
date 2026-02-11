package com.aivle.project.auth.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Cloudflare Turnstile 봇 검증 서비스.
 * WebClient를 사용하여 Cloudflare Turnstile API와 통신합니다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TurnstileService {

    private final WebClient webClient;

    @Value("${turnstile.secret-key}")
    private String secretKey;

    @Value("${turnstile.verify-url}")
    private String verifyUrl;

    @Value("${turnstile.timeout:5000}")
    private int timeoutMs;

    @Value("${turnstile.debug-enabled:false}")
    private boolean debugEnabled;

    @Value("${turnstile.mock-allow-all:false}")
    private boolean mockAllowAll;

    /**
     * Turnstile 토큰을 검증합니다.
     *
     * @param token 클라이언트로부터 받은 토큰
     * @param remoteIp 사용자 IP 주소 (선택사항)
     * @return 검증 성공 여부
     */
    public Mono<Boolean> verifyToken(String token, String remoteIp) {
        if (mockAllowAll) {
            logTurnstileDebug("Turnstile mock mode enabled. tokenPrefix={}, remoteIp={}", maskToken(token), remoteIp);
            return Mono.just(true);
        }

        if (!StringUtils.hasText(token)) {
            log.warn("Turnstile token is empty or blank");
            return Mono.just(false);
        }

        if (!StringUtils.hasText(secretKey)) {
            log.error("Turnstile secret key is not configured");
            return Mono.just(false);
        }

        TurnstileVerifyRequest request = new TurnstileVerifyRequest(
            secretKey,
            token.trim(),
            StringUtils.hasText(remoteIp) ? remoteIp : null
        );

        logTurnstileDebug(
            "Turnstile verify request. url={}, tokenPrefix={}, remoteIp={}, secretConfigured={}",
            verifyUrl,
            maskToken(token),
            remoteIp,
            StringUtils.hasText(secretKey)
        );

        return webClient.post()
            .uri(verifyUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(TurnstileVerifyResponse.class)
            .timeout(Duration.ofMillis(timeoutMs))
            .map(response -> {
                if (response.success()) {
                    logTurnstileDebug(
                        "Turnstile verification success. tokenPrefix={}, hostname={}, challengeTs={}, action={}, cdataPresent={}",
                        maskToken(token),
                        response.hostname(),
                        response.challenge_ts(),
                        response.action(),
                        StringUtils.hasText(response.cdata())
                    );
                    return true;
                } else {
                    logTurnstileDebug(
                        "Turnstile verification failed. tokenPrefix={}, errorCodes={}, hostname={}, challengeTs={}, action={}, cdataPresent={}",
                        maskToken(token),
                        response.errorCodes(),
                        response.hostname(),
                        response.challenge_ts(),
                        response.action(),
                        StringUtils.hasText(response.cdata())
                    );
                    return false;
                }
            })
            .onErrorResume(WebClientRequestException.class, e -> {
                log.error(
                    "Network error during Turnstile verification. url={}, tokenPrefix={}, remoteIp={}, message={}",
                    verifyUrl,
                    maskToken(token),
                    remoteIp,
                    e.getMessage()
                );
                return Mono.just(false);
            })
            .onErrorResume(WebClientResponseException.class, e -> {
                log.error("HTTP error during Turnstile verification. Status: {}, Body: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
                return Mono.just(false);
            })
            .onErrorResume(Exception.class, e -> {
                log.error("Unexpected error during Turnstile verification", e);
                return Mono.just(false);
            });
    }

    /**
     * 동기식 토큰 검증 (기존 동기 코드와의 호환성)
     *
     * @param token 클라이언트로부터 받은 토큰
     * @param remoteIp 사용자 IP 주소 (선택사항)
     * @return 검증 성공 여부
     */
    public boolean verifyTokenSync(String token, String remoteIp) {
        try {
            return verifyToken(token, remoteIp)
                .blockOptional(Duration.ofMillis(timeoutMs + 1000)) // 추가 버퍼 시간
                .orElse(false);
        } catch (Exception e) {
            log.error("Synchronous Turnstile verification failed", e);
            return false;
        }
    }

    /**
     * Turnstile 검증 요청 DTO
     */
    public record TurnstileVerifyRequest(
        String secret,
        String response,
        String remoteip
    ) {}

    /**
     * Turnstile 검증 응답 DTO
     */
    public record TurnstileVerifyResponse(
        boolean success,
        String challenge_ts,
        String hostname,
        @JsonProperty("error-codes")
        String[] errorCodes,
        String action,
        String cdata
    ) {}

    private void logTurnstileDebug(String message, Object... args) {
        if (debugEnabled) {
            log.info(message, args);
            return;
        }
        log.debug(message, args);
    }

    private String maskToken(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        int prefixLength = Math.min(10, token.length());
        return token.substring(0, prefixLength) + "...";
    }
}
