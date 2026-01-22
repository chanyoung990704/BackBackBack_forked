package com.aivle.project.auth.service;

import com.aivle.project.auth.token.JwtProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Access Token 블랙리스트 및 전체 로그아웃 기준 시각 관리.
 */
@Service
@RequiredArgsConstructor
public class AccessTokenBlacklistService {

	private static final String BLACKLIST_KEY = "blacklist:access:%s";
	private static final String LOGOUT_ALL_KEY = "logout-all:%s";

	private final StringRedisTemplate redisTemplate;
	private final JwtProperties jwtProperties;
	private final Clock clock = Clock.systemUTC();

	public void blacklist(String jti, Instant expiresAt) {
		if (jti == null || jti.isBlank() || expiresAt == null) {
			return;
		}
		Instant now = Instant.now(clock);
		Duration ttl = Duration.between(now, expiresAt);
		if (ttl.isZero() || ttl.isNegative()) {
			return;
		}
		redisTemplate.opsForValue().set(blacklistKey(jti), "1", ttl);
	}

	public void markLogoutAll(String userId, Instant logoutAt) {
		if (userId == null || userId.isBlank() || logoutAt == null) {
			return;
		}
		long ttlSeconds = jwtProperties.getAccessToken().getExpiration();
		if (ttlSeconds <= 0) {
			return;
		}
		redisTemplate.opsForValue().set(
			logoutAllKey(userId),
			String.valueOf(logoutAt.getEpochSecond()),
			Duration.ofSeconds(ttlSeconds)
		);
	}

	public boolean isBlacklisted(String jti) {
		if (jti == null || jti.isBlank()) {
			return false;
		}
		return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey(jti)));
	}

	public Instant getLogoutAllAt(String userId) {
		if (userId == null || userId.isBlank()) {
			return null;
		}
		String value = redisTemplate.opsForValue().get(logoutAllKey(userId));
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return Instant.ofEpochSecond(Long.parseLong(value));
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	private String blacklistKey(String jti) {
		return String.format(BLACKLIST_KEY, jti);
	}

	private String logoutAllKey(String userId) {
		return String.format(LOGOUT_ALL_KEY, userId);
	}
}
