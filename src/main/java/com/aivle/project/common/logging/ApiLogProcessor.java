package com.aivle.project.common.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 무거운 마스킹 및 로깅 작업을 비동기로 처리하는 프로세서.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ApiLogProcessor {

    private static final String MASKED_VALUE = "\"****\"";
    private static final String COOKIE_MASKED_VALUE = "\"[COOKIE_MASKED]\"";
    private static final Pattern JWT_PATTERN = Pattern.compile("^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$");
    private static final Pattern BEARER_PATTERN = Pattern.compile("(?i)^Bearer\\s+.+$");
    private static final Pattern COOKIE_PAIR_PATTERN = Pattern.compile("(?i)\\b([a-z0-9_-]+)\\s*=\\s*[^;]+");
    private static final Pattern SENSITIVE_COOKIE_KEY_PATTERN = Pattern.compile("(?i).*(token|session|auth|jwt|csrf|cookie).*");
    private static final Pattern SENSITIVE_JSON_PATTERN = Pattern.compile(
        "\"(?i)([^\"\\\\]*(password|token|secret|credential|authorization|cookie|session|jwt|csrf|name|phone|ssn|creditcard)[^\"\\\\]*)\"\\s*:\\s*\"[^\"]*\""
    );

    private final ObjectMapper objectMapper;

    @Async
    public void processLog(String method, String uri, String className, String methodName, 
                           Object[] args, Object result, long time, Map<String, String> contextMap) {
        if (contextMap != null) {
            MDC.setContextMap(contextMap);
        }

        try {
            String maskedArgs = maskArgs(args);
            
            if (result instanceof Throwable e) {
                log.error("API Error: [{} {}] | Method: {}.{} | Error: {} | Time: {}ms | RequestId: {}",
                    method, uri, className, methodName, e.getMessage(), time, MDC.get("requestId"));
            } else {
                log.info("API Request: [{} {}] | Method: {}.{} | Args: {} | Time: {}ms | RequestId: {}",
                    method, uri, className, methodName, maskedArgs, time, MDC.get("requestId"));
            }
        } finally {
            MDC.clear();
        }
    }

    public String maskArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        return IntStream.range(0, args.length)
            .mapToObj(index -> mask(args[index]))
            .collect(Collectors.joining(", ", "[", "]"));
    }

    private String mask(Object arg) {
        if (arg == null) return "null";

        // HTTP 관련 객체 처리
        if (arg instanceof jakarta.servlet.http.HttpServletRequest || arg instanceof jakarta.servlet.http.HttpServletResponse) {
            return arg.getClass().getSimpleName();
        }

        // MultipartFile 처리
        if (arg instanceof org.springframework.web.multipart.MultipartFile file) {
            return String.format("MultipartFile(name=%s, size=%d)", file.getOriginalFilename(), file.getSize());
        }

        // 대용량 데이터 타입 처리
        if (arg instanceof java.util.Collection<?> col) {
            return String.format("Collection(size=%d)", col.size());
        }
        if (arg instanceof java.util.Map<?, ?> map) {
            return String.format("Map(size=%d)", map.size());
        }
        if (arg.getClass().getName().contains("org.springframework.data.domain.Page")) {
            return "PageObject";
        }

        // String 타입 처리
        if (arg instanceof String str) {
            return maskString(str);
        }

        // 숫자 타입 처리
        if (arg instanceof Number) {
            return arg.toString();
        }

        // 일반 DTO 객체
        try {
            String json = objectMapper.writeValueAsString(arg);
            if (json.length() > 2048) {
                return String.format("%s(large_json, size=%d)", arg.getClass().getSimpleName(), json.length());
            }
            return maskJson(json);
        } catch (Exception e) {
            return "[COMPLEX_OBJECT]";
        }
    }

    private String maskString(String str) {
        String normalized = str.trim();
        if (BEARER_PATTERN.matcher(normalized).matches()) return "\"Bearer ****\"";
        if (JWT_PATTERN.matcher(normalized).matches()) return MASKED_VALUE;
        if (containsSensitiveCookiePair(normalized)) return COOKIE_MASKED_VALUE;
        if (normalized.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            return "\"" + maskEmail(normalized) + "\"";
        }
        try {
            return objectMapper.writeValueAsString(str);
        } catch (Exception e) {
            return "\"[MASKED]\"";
        }
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex > 2) return email.substring(0, 2) + "***" + email.substring(atIndex);
        return "***@***";
    }

    private String maskJson(String json) {
        Pattern emailPattern = Pattern.compile("\"(?i)email\"\\s*:\\s*\"([^\"]+)\"");
        Matcher emailMatcher = emailPattern.matcher(json);
        StringBuffer sb = new StringBuffer();
        while (emailMatcher.find()) {
            emailMatcher.appendReplacement(sb, "\"email\":\"" + maskEmail(emailMatcher.group(1)) + "\"");
        }
        emailMatcher.appendTail(sb);
        json = sb.toString();

        Matcher sensitiveMatcher = SENSITIVE_JSON_PATTERN.matcher(json);
        StringBuffer result = new StringBuffer();
        while (sensitiveMatcher.find()) {
            String replacement = "\"" + sensitiveMatcher.group(1) + "\":\"****\"";
            sensitiveMatcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        sensitiveMatcher.appendTail(result);
        return result.toString();
    }

    private boolean containsSensitiveCookiePair(String value) {
        Matcher matcher = COOKIE_PAIR_PATTERN.matcher(value);
        while (matcher.find()) {
            if (SENSITIVE_COOKIE_KEY_PATTERN.matcher(matcher.group(1)).matches()) return true;
        }
        return false;
    }
}
