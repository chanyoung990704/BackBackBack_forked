package com.aivle.project.auth.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.aivle.project.auth.service.AccessTokenBlacklistService;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class AccessTokenValidatorTest {

	@Mock
	private AccessTokenBlacklistService accessTokenBlacklistService;

	@Test
	@DisplayName("블랙리스트에 등록된 토큰이면 검증에 실패한다")
	void validate_shouldFailWhenBlacklisted() {
		// given: 블랙리스트된 토큰을 준비
		Instant now = Instant.now();
		Jwt jwt = Jwt.withTokenValue("access")
			.header("alg", "RS256")
			.subject("user-uuid")
			.issuedAt(now.minusSeconds(10))
			.expiresAt(now.plusSeconds(60))
			.jti("jti-1")
			.build();
		when(accessTokenBlacklistService.isBlacklisted("jti-1")).thenReturn(true);

		// when: 토큰을 검증
		OAuth2TokenValidatorResult result = new AccessTokenValidator(accessTokenBlacklistService).validate(jwt);

		// then: 실패 결과가 반환된다
		assertThat(result.hasErrors()).isTrue();
	}

	@Test
	@DisplayName("전체 로그아웃 기준 이전 발급 토큰이면 실패한다")
	void validate_shouldFailWhenLogoutAllAfterIssuedAt() {
		// given: 전체 로그아웃 기준이 존재하는 토큰을 준비
		Instant issuedAt = Instant.now().minusSeconds(30);
		Jwt jwt = Jwt.withTokenValue("access")
			.header("alg", "RS256")
			.subject("user-uuid")
			.issuedAt(issuedAt)
			.expiresAt(Instant.now().plusSeconds(60))
			.jti("jti-2")
			.build();
		when(accessTokenBlacklistService.isBlacklisted("jti-2")).thenReturn(false);
		when(accessTokenBlacklistService.getLogoutAllAt("user-uuid")).thenReturn(Instant.now());

		// when: 토큰을 검증
		OAuth2TokenValidatorResult result = new AccessTokenValidator(accessTokenBlacklistService).validate(jwt);

		// then: 실패 결과가 반환된다
		assertThat(result.hasErrors()).isTrue();
	}

	@Test
	@DisplayName("정상 토큰이면 검증에 성공한다")
	void validate_shouldPassWhenTokenValid() {
		// given: 유효한 토큰을 준비
		Instant now = Instant.now();
		Jwt jwt = Jwt.withTokenValue("access")
			.header("alg", "RS256")
			.subject("user-uuid")
			.issuedAt(now)
			.expiresAt(now.plusSeconds(60))
			.jti("jti-3")
			.build();
		when(accessTokenBlacklistService.isBlacklisted("jti-3")).thenReturn(false);
		when(accessTokenBlacklistService.getLogoutAllAt("user-uuid")).thenReturn(null);

		// when: 토큰을 검증
		OAuth2TokenValidatorResult result = new AccessTokenValidator(accessTokenBlacklistService).validate(jwt);

		// then: 성공 결과가 반환된다
		assertThat(result.hasErrors()).isFalse();
	}

	@Test
	@DisplayName("전체 로그아웃 기준과 동일한 시각의 토큰은 허용한다")
	void validate_shouldAllowWhenIssuedAtEqualsLogoutAllAt() {
		// given: 로그아웃 기준과 동일한 발급 시각을 가진 토큰을 준비
		Instant now = Instant.now();
		Jwt jwt = Jwt.withTokenValue("access")
			.header("alg", "RS256")
			.subject("user-uuid")
			.issuedAt(now)
			.expiresAt(now.plusSeconds(60))
			.jti("jti-4")
			.build();
		when(accessTokenBlacklistService.isBlacklisted("jti-4")).thenReturn(false);
		when(accessTokenBlacklistService.getLogoutAllAt("user-uuid")).thenReturn(now);

		// when: 토큰을 검증
		OAuth2TokenValidatorResult result = new AccessTokenValidator(accessTokenBlacklistService).validate(jwt);

		// then: 성공 결과가 반환된다
		assertThat(result.hasErrors()).isFalse();
	}
}
