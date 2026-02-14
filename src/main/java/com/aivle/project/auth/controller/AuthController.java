package com.aivle.project.auth.controller;

import com.aivle.project.auth.config.AuthCookieProperties;
import com.aivle.project.auth.dto.AuthLoginResponse;
import com.aivle.project.auth.dto.LoginRequest;
import com.aivle.project.auth.dto.PasswordChangeRequest;
import com.aivle.project.auth.dto.SignupRequest;
import com.aivle.project.auth.dto.SignupResponse;
import com.aivle.project.auth.dto.TokenRefreshRequest;
import com.aivle.project.auth.dto.TokenResponse;
import com.aivle.project.auth.exception.AuthErrorCode;
import com.aivle.project.auth.exception.AuthException;
import com.aivle.project.auth.service.AuthService;
import com.aivle.project.auth.service.SignUpService;
import com.aivle.project.common.dto.ApiResponse;
import com.aivle.project.common.security.CurrentUser;
import com.aivle.project.user.entity.UserEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.security.SecureRandom;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 관련 API.
 */
@Tag(name = "인증", description = "로그인/회원가입/토큰 재발급 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

	private static final String CSRF_COOKIE_NAME = "csrf_token";
	private static final String CSRF_HEADER_NAME = "X-CSRF-Token";
	private static final int CSRF_TOKEN_BYTES = 32;

	private final AuthService authService;
	private final SignUpService signUpService;
	private final AuthCookieProperties authCookieProperties;
	private final SecureRandom secureRandom = new SecureRandom();

	@PostMapping("/login")
	@Operation(summary = "로그인", description = "이메일/비밀번호로 로그인하고 토큰을 발급합니다.", security = {})
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공",
			content = @Content(schema = @Schema(implementation = AuthLoginResponse.class))),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청값 오류"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
	})
	public ResponseEntity<ApiResponse<AuthLoginResponse>> login(
		@Valid @RequestBody LoginRequest request,
		@Parameter(hidden = true) HttpServletRequest httpServletRequest
	) {
		String ipAddress = resolveIp(httpServletRequest);
		AuthLoginResponse response = authService.login(request, ipAddress);
		
		ResponseCookie cookie = createRefreshTokenCookie(response.refreshToken(), response.refreshExpiresIn());
		ResponseCookie csrfCookie = createCsrfCookie(generateCsrfToken(), response.refreshExpiresIn());
		
		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, cookie.toString())
			.header(HttpHeaders.SET_COOKIE, csrfCookie.toString())
			.body(ApiResponse.ok(response));
	}

	@PostMapping("/refresh")
	@Operation(summary = "토큰 재발급", description = "리프레시 토큰으로 액세스 토큰을 재발급합니다.", security = {})
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "재발급 성공",
			content = @Content(schema = @Schema(implementation = TokenResponse.class))),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청값 오류"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "재발급 실패"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
	})
	public ResponseEntity<ApiResponse<TokenResponse>> refresh(
		@Parameter(hidden = true) @CookieValue(name = "refresh_token", required = false) String cookieRefreshToken,
		@Parameter(hidden = true) @CookieValue(name = CSRF_COOKIE_NAME, required = false) String csrfCookieToken,
		@Parameter(hidden = true) @RequestHeader(name = CSRF_HEADER_NAME, required = false) String csrfHeaderToken
	) {
		validateCsrf(csrfCookieToken, csrfHeaderToken);
		String refreshToken = cookieRefreshToken;

		if (refreshToken == null || refreshToken.isBlank()) {
			throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
		}

		TokenRefreshRequest refreshRequest = new TokenRefreshRequest();
		refreshRequest.setRefreshToken(refreshToken);
		
		TokenResponse response = authService.refresh(refreshRequest);
		
		ResponseCookie cookie = createRefreshTokenCookie(response.refreshToken(), response.refreshExpiresIn());
		ResponseCookie csrfCookie = createCsrfCookie(generateCsrfToken(), response.refreshExpiresIn());
		
		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, cookie.toString())
			.header(HttpHeaders.SET_COOKIE, csrfCookie.toString())
			.body(ApiResponse.ok(response));
	}

	@PostMapping("/logout")
	@Operation(summary = "로그아웃", description = "리프레시 토큰을 무효화하고 쿠키를 삭제합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
	})
	public ResponseEntity<ApiResponse<Void>> logout(
		@Parameter(hidden = true) @CookieValue(name = "refresh_token", required = false) String cookieRefreshToken,
		@Parameter(hidden = true) @CookieValue(name = CSRF_COOKIE_NAME, required = false) String csrfCookieToken,
		@Parameter(hidden = true) @RequestHeader(name = CSRF_HEADER_NAME, required = false) String csrfHeaderToken,
		@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
	) {
		validateCsrf(csrfCookieToken, csrfHeaderToken);
		authService.logout(cookieRefreshToken, jwt);
		ResponseCookie cookie = createRefreshTokenCookie("", 0);
		ResponseCookie csrfCookie = createCsrfCookie("", 0);
		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, cookie.toString())
			.header(HttpHeaders.SET_COOKIE, csrfCookie.toString())
			.body(ApiResponse.ok());
	}

	@PostMapping("/logout-all")
	@Operation(summary = "전체 로그아웃", description = "사용자의 모든 기기에서 로그아웃 처리합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "전체 로그아웃 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
	})
	public ResponseEntity<ApiResponse<Void>> logoutAll(@Parameter(hidden = true) @CurrentUser UserEntity user) {
		authService.logoutAll(user);
		ResponseCookie cookie = createRefreshTokenCookie("", 0);
		ResponseCookie csrfCookie = createCsrfCookie("", 0);
		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, cookie.toString())
			.header(HttpHeaders.SET_COOKIE, csrfCookie.toString())
			.body(ApiResponse.ok());
	}

	@PostMapping("/change-password")
	@Operation(summary = "비밀번호 변경", description = "로그인된 사용자의 비밀번호를 변경합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "변경 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 비밀번호"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
	})
	public ResponseEntity<ApiResponse<Void>> changePassword(
		@Parameter(hidden = true) @CurrentUser UserEntity user,
		@Valid @RequestBody PasswordChangeRequest request) {
		authService.changePassword(user, request);
		return ResponseEntity.ok(ApiResponse.ok());
	}

	@PostMapping("/signup")
	@Operation(summary = "회원가입", description = "신규 회원을 등록합니다.", security = {})
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "회원가입 성공",
			content = @Content(schema = @Schema(implementation = SignupResponse.class))),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청값 오류"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
	})
	public ResponseEntity<ApiResponse<SignupResponse>> signup(
		@Valid @RequestBody SignupRequest request,
		@Parameter(hidden = true) HttpServletRequest httpServletRequest
	) {
		String clientIp = resolveIp(httpServletRequest);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(signUpService.signup(request, clientIp)));
	}

	private ResponseCookie createRefreshTokenCookie(String refreshToken, long maxAgeSeconds) {
		String sameSite = authCookieProperties.getSameSite();
		if (sameSite == null || sameSite.isBlank()) {
			sameSite = "Strict";
		}
		return ResponseCookie.from("refresh_token", refreshToken)
			.httpOnly(true)
			.secure(authCookieProperties.isSecure())
			.path("/")
			.maxAge(maxAgeSeconds)
			.sameSite(sameSite)
			.build();
	}

	private ResponseCookie createCsrfCookie(String csrfToken, long maxAgeSeconds) {
		String sameSite = authCookieProperties.getSameSite();
		if (sameSite == null || sameSite.isBlank()) {
			sameSite = "Strict";
		}
		return ResponseCookie.from(CSRF_COOKIE_NAME, csrfToken)
			.httpOnly(false)
			.secure(authCookieProperties.isSecure())
			.path("/")
			.maxAge(maxAgeSeconds)
			.sameSite(sameSite)
			.build();
	}

	private String generateCsrfToken() {
		byte[] bytes = new byte[CSRF_TOKEN_BYTES];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private void validateCsrf(String cookieToken, String headerToken) {
		if (cookieToken == null || cookieToken.isBlank()) {
			throw new AuthException(AuthErrorCode.CSRF_VALIDATION_FAILED);
		}
		if (headerToken == null || headerToken.isBlank()) {
			throw new AuthException(AuthErrorCode.CSRF_VALIDATION_FAILED);
		}
		if (!cookieToken.equals(headerToken)) {
			throw new AuthException(AuthErrorCode.CSRF_VALIDATION_FAILED);
		}
	}

	private String resolveIp(HttpServletRequest request) {
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			return forwarded.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}
}
