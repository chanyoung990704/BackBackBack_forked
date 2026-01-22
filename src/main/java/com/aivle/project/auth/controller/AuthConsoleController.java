package com.aivle.project.auth.controller;

import com.aivle.project.common.dto.ApiResponse;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 로그인/로그아웃 검증용 콘솔 페이지 (dev 전용).
 */
@Profile("dev")
@Controller
@RequestMapping("/auth/console")
public class AuthConsoleController {

	@GetMapping
	public String console() {
		return "auth-console";
	}

	@GetMapping("/claims")
	@ResponseBody
	public ApiResponse<Map<String, Object>> claims(@AuthenticationPrincipal Jwt jwt) {
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("sub", jwt.getSubject());
		response.put("email", jwt.getClaimAsString("email"));
		response.put("roles", jwt.getClaimAsStringList("roles"));
		response.put("deviceId", jwt.getClaimAsString("deviceId"));
		response.put("issuer", jwt.getClaimAsString("iss"));
		response.put("jti", jwt.getId());
		response.put("issuedAt", formatInstant(jwt.getIssuedAt()));
		response.put("expiresAt", formatInstant(jwt.getExpiresAt()));
		return ApiResponse.ok(response);
	}

	private String formatInstant(Instant instant) {
		if (instant == null) {
			return null;
		}
		return instant.toString();
	}
}
