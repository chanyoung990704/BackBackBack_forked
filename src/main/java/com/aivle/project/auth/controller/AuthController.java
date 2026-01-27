package com.aivle.project.auth.controller;

import com.aivle.project.auth.dto.LoginRequest;
import com.aivle.project.auth.dto.SignupRequest;
import com.aivle.project.auth.dto.SignupResponse;
import com.aivle.project.auth.dto.TokenRefreshRequest;
import com.aivle.project.auth.dto.TokenResponse;
import com.aivle.project.auth.service.AuthService;
import com.aivle.project.auth.service.SignUpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 관련 API.
 */
@Tag(name = "인증", description = "로그인/회원가입/토큰 재발급 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

	private final AuthService authService;
	private final SignUpService signUpService;

	@PostMapping("/login")
	@Operation(summary = "로그인", description = "이메일/비밀번호로 로그인하고 토큰을 발급합니다.", security = {})
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "로그인 성공",
			content = @Content(schema = @Schema(implementation = TokenResponse.class))),
		@ApiResponse(responseCode = "400", description = "요청값 오류"),
		@ApiResponse(responseCode = "401", description = "인증 실패"),
		@ApiResponse(responseCode = "500", description = "서버 오류")
	})
	public ResponseEntity<TokenResponse> login(
		@Valid @RequestBody LoginRequest request,
		@Parameter(hidden = true) HttpServletRequest httpServletRequest
	) {
		String ipAddress = resolveIp(httpServletRequest);
		return ResponseEntity.ok(authService.login(request, ipAddress));
	}

	@PostMapping("/refresh")
	@Operation(summary = "토큰 재발급", description = "리프레시 토큰으로 액세스 토큰을 재발급합니다.", security = {})
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "재발급 성공",
			content = @Content(schema = @Schema(implementation = TokenResponse.class))),
		@ApiResponse(responseCode = "400", description = "요청값 오류"),
		@ApiResponse(responseCode = "401", description = "재발급 실패"),
		@ApiResponse(responseCode = "500", description = "서버 오류")
	})
	public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody TokenRefreshRequest request) {
		return ResponseEntity.ok(authService.refresh(request));
	}

	@PostMapping("/signup")
	@Operation(summary = "회원가입", description = "신규 회원을 등록합니다.", security = {})
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "회원가입 성공",
			content = @Content(schema = @Schema(implementation = SignupResponse.class))),
		@ApiResponse(responseCode = "400", description = "요청값 오류"),
		@ApiResponse(responseCode = "500", description = "서버 오류")
	})
	public ResponseEntity<SignupResponse> signup(
			@Valid @RequestBody SignupRequest request,
			@Parameter(hidden = true) HttpServletRequest httpServletRequest) {
		String clientIp = resolveIp(httpServletRequest);
		return ResponseEntity.status(HttpStatus.CREATED).body(signUpService.signup(request, clientIp));
	}

	private String resolveIp(HttpServletRequest request) {
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			return forwarded.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}
}
