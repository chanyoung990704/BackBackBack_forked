package com.aivle.project.auth.controller;

import com.aivle.project.auth.dto.LoginRequest;
import com.aivle.project.auth.dto.SignupRequest;
import com.aivle.project.auth.dto.SignupResponse;
import com.aivle.project.auth.dto.TokenRefreshRequest;
import com.aivle.project.auth.dto.TokenResponse;
import com.aivle.project.auth.service.AuthService;
import com.aivle.project.auth.service.SignUpService;
import com.aivle.project.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 관련 API.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

	private final AuthService authService;
	private final SignUpService signUpService;

	@PostMapping("/login")
	public ResponseEntity<ApiResponse<TokenResponse>> login(
		@Valid @RequestBody LoginRequest request,
		HttpServletRequest httpServletRequest
	) {
		String ipAddress = resolveIp(httpServletRequest);
		return ResponseEntity.ok(ApiResponse.ok(authService.login(request, ipAddress)));
	}

	@PostMapping("/refresh")
	public ResponseEntity<ApiResponse<TokenResponse>> refresh(@Valid @RequestBody TokenRefreshRequest request) {
		return ResponseEntity.ok(ApiResponse.ok(authService.refresh(request)));
	}

	@PostMapping("/signup")
	public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(signUpService.signup(request)));
	}

	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal Jwt jwt) {
		authService.logout(jwt);
		return ResponseEntity.ok(ApiResponse.ok());
	}

	@PostMapping("/logout-all")
	public ResponseEntity<ApiResponse<Void>> logoutAll(@AuthenticationPrincipal Jwt jwt) {
		authService.logoutAll(jwt);
		return ResponseEntity.ok(ApiResponse.ok());
	}

	private String resolveIp(HttpServletRequest request) {
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			return forwarded.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}
}
