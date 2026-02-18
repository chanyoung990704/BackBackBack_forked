package com.aivle.project.common.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ApiLoggingAspectTest {

	private ApiLogProcessor apiLogProcessor;

	@BeforeEach
	void setUp() {
		apiLogProcessor = new ApiLogProcessor(new ObjectMapper());
	}

	@Test
	@DisplayName("비밀번호 필드는 마스킹되어야 한다")
	void mask_shouldMaskPassword() {
		Map<String, String> map = new HashMap<>();
		map.put("email", "test@example.com");
		map.put("password", "secret123");

		String result = apiLogProcessor.maskArgs(new Object[]{map});

		assertThat(result).contains("Map(size=2)");
		assertThat(result).doesNotContain("secret123");
	}

	@Test
	@DisplayName("토큰 필드는 마스킹되어야 한다")
	void mask_shouldMaskTokens() {
		Map<String, String> map = new HashMap<>();
		map.put("accessToken", "abc.def.ghi");
		map.put("refreshToken", "xyz.uvw.rst");

		String result = apiLogProcessor.maskArgs(new Object[]{map});

		assertThat(result).contains("Map(size=2)");
		assertThat(result).doesNotContain("abc.def.ghi");
		assertThat(result).doesNotContain("xyz.uvw.rst");
	}

	@Test
	@DisplayName("단순 문자열이나 숫자는 마스킹되지 않아야 한다")
	void mask_shouldNotMaskSimpleTypes() {
		assertThat(apiLogProcessor.maskArgs(new Object[]{"hello"})).isEqualTo("[\"hello\"]");
		assertThat(apiLogProcessor.maskArgs(new Object[]{123})).isEqualTo("[123]");
	}

	@Test
	@DisplayName("Bearer 토큰 문자열은 마스킹되어야 한다")
	void mask_shouldMaskBearerTokenString() {
		String result = apiLogProcessor.maskArgs(new Object[]{"Bearer eyJhbGciOiJIUzI1NiJ9.payload.signature"});
		assertThat(result).isEqualTo("[\"Bearer ****\"]");
	}

	@Test
	@DisplayName("JWT 토큰 문자열은 마스킹되어야 한다")
	void mask_shouldMaskJwtTokenString() {
		String result = apiLogProcessor.maskArgs(new Object[]{"eyJhbGciOiJIUzI1NiJ9.payload.signature"});
		assertThat(result).isEqualTo("[\"****\"]");
	}

	@Test
	@DisplayName("Cookie 문자열에 민감 키가 있으면 마스킹되어야 한다")
	void mask_shouldMaskCookieStringWithSensitiveKey() {
		String cookieHeader = "refresh_token=abc.def.ghi; theme=light";
		String result = apiLogProcessor.maskArgs(new Object[]{cookieHeader});
		assertThat(result).isEqualTo("[\"[COOKIE_MASKED]\"]");
	}

	@Test
	@DisplayName("중첩된 객체의 민감 정보도 마스킹되어야 한다")
	void mask_shouldMaskNestedSensitiveData() {
		Map<String, Object> nested = new HashMap<>();
		nested.put("password", "inner-secret");
		
		Map<String, Object> root = new HashMap<>();
		root.put("user", nested);
		root.put("token", "outer-token");

		String result = apiLogProcessor.maskArgs(new Object[]{root});

		assertThat(result).contains("Map(size=2)");
		assertThat(result).doesNotContain("inner-secret");
		assertThat(result).doesNotContain("outer-token");
	}
}
