package com.aivle.project.auth.service;

import com.aivle.project.auth.exception.AuthErrorCode;
import com.aivle.project.auth.exception.AuthException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
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
		Long count = redisTemplate.opsForValue().increment(key);
		if (count == null) {
			return false;
		}
		if (count == 1L) {
			redisTemplate.expire(key, WINDOW);
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
