package com.aivle.project.auth.controller;

import com.aivle.project.auth.dto.EmailVerificationResultResponse;
import com.aivle.project.common.dto.ApiResponse;
import com.aivle.project.common.error.ErrorResponse;
import com.aivle.project.user.service.EmailVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 이메일 인증 컨트롤러.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "인증", description = "이메일 인증 API")
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    @Value("${app.email.verification.redirect-base-url:}")
    private String redirectBaseUrl;

    /**
     * 이메일 인증 처리.
     */
    @GetMapping("/verify-email")
    @Operation(summary = "이메일 인증", description = "이메일 인증 토큰을 검증합니다.", security = {})
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인증 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "인증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> verifyEmail(
        @Parameter(description = "이메일 인증 토큰", example = "a1b2c3d4")
        @RequestParam String token,
        @Parameter(description = "프론트 리다이렉트 여부", example = "true")
        @RequestParam(required = false, defaultValue = "false") boolean redirect,
        @Parameter(description = "인증 결과를 전달받을 프론트 복귀 URL", example = "https://front.example.com/auth/signup")
        @RequestParam(required = false) String returnUrl,
        @Parameter(hidden = true) HttpServletRequest request
    ) {
        try {
            emailVerificationService.verifyEmail(token);
            if (redirect) {
                return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(buildRedirectUrl(VerificationRedirectStatus.SUCCESS, returnUrl)))
                    .build();
            }
            return ResponseEntity.ok(ApiResponse.ok(successResponse("이메일 인증이 완료되었습니다.")));
        } catch (IllegalArgumentException e) {
            log.warn("이메일 인증 실패: {}", e.getMessage());
            VerificationRedirectStatus status = mapStatus(e.getMessage());
            if (redirect) {
                return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(buildRedirectUrl(status, returnUrl)))
                    .build();
            }
            return ResponseEntity.badRequest().body(ApiResponse.fail(
                ErrorResponse.of("EMAIL_VERIFICATION_" + status.name(), e.getMessage(), request.getRequestURI())
            ));
        } catch (Exception e) {
            log.error("이메일 인증 처리 중 서버 오류", e);
            if (redirect) {
                return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(buildRedirectUrl(VerificationRedirectStatus.ERROR, returnUrl)))
                    .build();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail(
                ErrorResponse.of(
                    "EMAIL_VERIFICATION_ERROR",
                    "이메일 인증 처리 중 오류가 발생했습니다.",
                    request.getRequestURI()
                )
            ));
        }
    }

    /**
     * 이메일 인증 재전송.
     */
    @GetMapping("/resend-verification")
    @Operation(summary = "이메일 인증 재전송", description = "사용자에게 인증 메일을 재전송합니다.", security = {})
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "재전송 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "재전송 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<EmailVerificationResultResponse>> resendVerification(
        @Parameter(description = "사용자 ID", example = "1")
        @RequestParam Long userId
    ) {
        try {
            emailVerificationService.resendVerificationEmail(userId);
            return ResponseEntity.ok(ApiResponse.ok(successResponse("인증 이메일이 재전송되었습니다.")));
        } catch (IllegalStateException | IllegalArgumentException e) {
            log.warn("인증 이메일 재전송 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.fail(
                ErrorResponse.of("EMAIL_VERIFICATION_RESEND_FAILED", e.getMessage(), "/api/auth/resend-verification")
            ));
        }
    }

    private EmailVerificationResultResponse successResponse(String message) {
        return new EmailVerificationResultResponse(VerificationRedirectStatus.SUCCESS.value, message);
    }

    private String buildRedirectUrl(VerificationRedirectStatus status, String returnUrl) {
        String redirectTarget = resolveReturnUrl(returnUrl).orElseGet(this::defaultRedirectPath);
        return UriComponentsBuilder.fromUriString(redirectTarget)
            .queryParam("status", status.value)
            .toUriString();
    }

    private Optional<String> resolveReturnUrl(String returnUrl) {
        if (!StringUtils.hasText(returnUrl)) {
            return Optional.empty();
        }

        try {
            String normalizedBaseUrl = normalizedBaseUrl();
            URI baseUri = URI.create(normalizedBaseUrl);
            URI requestUri = URI.create(returnUrl);

            URI resolvedUri;
            if (requestUri.isAbsolute()) {
                resolvedUri = requestUri;
            } else {
                // 상대 경로는 프론트 기본 URL 하위 경로로 제한한다.
                if (!returnUrl.startsWith("/")) {
                    return Optional.empty();
                }
                resolvedUri = baseUri.resolve(returnUrl);
            }

            if (!sameOrigin(baseUri, resolvedUri)) {
                return Optional.empty();
            }
            if (!isSubPath(baseUri, resolvedUri)) {
                return Optional.empty();
            }
            return Optional.of(resolvedUri.toString());
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private String defaultRedirectPath() {
        return UriComponentsBuilder.fromUriString(normalizedBaseUrl())
            .path("/auth/verify-email")
            .toUriString();
    }

    private String normalizedBaseUrl() {
        String baseUrl = StringUtils.hasText(redirectBaseUrl)
            ? redirectBaseUrl
            : ServletUriComponentsBuilder.fromCurrentContextPath().toUriString();
        return baseUrl.endsWith("/")
            ? baseUrl.substring(0, baseUrl.length() - 1)
            : baseUrl;
    }

    private boolean sameOrigin(URI baseUri, URI targetUri) {
        int basePort = baseUri.getPort() == -1 ? defaultPort(baseUri.getScheme()) : baseUri.getPort();
        int targetPort = targetUri.getPort() == -1 ? defaultPort(targetUri.getScheme()) : targetUri.getPort();
        return safeEquals(baseUri.getScheme(), targetUri.getScheme())
            && safeEquals(baseUri.getHost(), targetUri.getHost())
            && basePort == targetPort;
    }

    private boolean isSubPath(URI baseUri, URI targetUri) {
        String basePath = normalizedPath(baseUri.getPath());
        String targetPath = normalizedPath(targetUri.getPath());
        return targetPath.equals(basePath) || targetPath.startsWith(basePath + "/");
    }

    private String normalizedPath(String path) {
        if (!StringUtils.hasText(path) || "/".equals(path)) {
            return "";
        }
        String normalized = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        return normalized.startsWith("/") ? normalized : "/" + normalized;
    }

    private int defaultPort(String scheme) {
        if ("https".equalsIgnoreCase(scheme)) {
            return 443;
        }
        if ("http".equalsIgnoreCase(scheme)) {
            return 80;
        }
        return -1;
    }

    private boolean safeEquals(String left, String right) {
        return left != null && left.equalsIgnoreCase(right);
    }

    private VerificationRedirectStatus mapStatus(String message) {
        if (message == null) {
            return VerificationRedirectStatus.INVALID;
        }
        if (message.contains("만료")) {
            return VerificationRedirectStatus.EXPIRED;
        }
        if (message.contains("이미")) {
            return VerificationRedirectStatus.ALREADY_VERIFIED;
        }
        return VerificationRedirectStatus.INVALID;
    }

    private enum VerificationRedirectStatus {
        SUCCESS("success"),
        EXPIRED("expired"),
        INVALID("invalid"),
        ALREADY_VERIFIED("already_verified"),
        ERROR("error");

        private final String value;

        VerificationRedirectStatus(String value) {
            this.value = value;
        }
    }
}
