package com.aivle.project.auth.service;

import com.aivle.project.auth.exception.AuthErrorCode;
import com.aivle.project.auth.exception.AuthException;
import java.time.Duration;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 이메일 인증 재전송 요청 횟수 제한.
 */
@Component
@RequiredArgsConstructor
public class VerificationResendRateLimiter {

	private static final String USER_KEY_PATTERN = "resend-verification:user:%s";
	private static final String IP_KEY_PATTERN = "resend-verification:ip:%s";
	private static final int USER_LIMIT = 3;
	private static final int IP_LIMIT = 10;
	private static final Duration WINDOW = Duration.ofMinutes(10);

	private static final String INCREMENT_AND_EXPIRE_LUA = 
		"local current = redis.call('incr', KEYS[1]) " +
		"if current == 1 then " +
		"    redis.call('pexpire', KEYS[1], ARGV[1]) " +
		"end " +
		"return current";

	private final StringRedisTemplate redisTemplate;

	public void checkLimit(Long userId, String clientIp) {
		if (userId != null && !acquire(userKey(userId), USER_LIMIT)) {
			throw new AuthException(AuthErrorCode.RESEND_VERIFICATION_RATE_LIMITED);
		}
		if (StringUtils.hasText(clientIp) && !acquire(ipKey(clientIp), IP_LIMIT)) {
			throw new AuthException(AuthErrorCode.RESEND_VERIFICATION_RATE_LIMITED);
		}
	}

	private boolean acquire(String key, int limit) {
		DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(INCREMENT_AND_EXPIRE_LUA, Long.class);
		Long count = redisTemplate.execute(redisScript, Collections.singletonList(key), String.valueOf(WINDOW.toMillis()));
		if (count == null) {
			return false;
		}
		return count <= limit;
	}

	private String userKey(Long userId) {
		return USER_KEY_PATTERN.formatted(userId);
	}

	private String ipKey(String ip) {
		return IP_KEY_PATTERN.formatted(ip);
	}
}
