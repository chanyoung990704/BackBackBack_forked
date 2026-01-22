package com.aivle.project.auth.token;

import com.aivle.project.auth.service.AccessTokenBlacklistService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Access Token 블랙리스트 및 전체 로그아웃 기준 검증.
 */
@RequiredArgsConstructor
public class AccessTokenValidator implements OAuth2TokenValidator<Jwt> {

	private static final OAuth2Error INVALID_TOKEN = new OAuth2Error("invalid_token", "유효하지 않은 토큰입니다.", null);

	private final AccessTokenBlacklistService accessTokenBlacklistService;

	@Override
	public OAuth2TokenValidatorResult validate(Jwt jwt) {
		String jti = jwt.getId();
		if (accessTokenBlacklistService.isBlacklisted(jti)) {
			return OAuth2TokenValidatorResult.failure(INVALID_TOKEN);
		}

		String userId = jwt.getSubject();
		Instant logoutAllAt = accessTokenBlacklistService.getLogoutAllAt(userId);
		if (logoutAllAt == null) {
			return OAuth2TokenValidatorResult.success();
		}

		Instant issuedAt = jwt.getIssuedAt();
		if (issuedAt == null || issuedAt.isBefore(logoutAllAt)) {
			return OAuth2TokenValidatorResult.failure(INVALID_TOKEN);
		}
		return OAuth2TokenValidatorResult.success();
	}
}
