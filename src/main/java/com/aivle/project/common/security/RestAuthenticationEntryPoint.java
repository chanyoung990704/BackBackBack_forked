package com.aivle.project.common.security;

import com.aivle.project.common.dto.ApiResponse;
import com.aivle.project.common.error.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * 인증 실패 응답을 JSON 형태로 반환.
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper;

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
		throws IOException {
		ErrorResponse errorResponse = ErrorResponse.of(
			"AUTH_401",
			"인증에 실패했습니다. (토큰 만료 또는 유효하지 않음)",
			request.getRequestURI()
		);
		ApiResponse<Void> apiResponse = ApiResponse.fail(errorResponse);

		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), apiResponse);
	}
}
