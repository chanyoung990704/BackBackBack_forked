package com.aivle.project.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aivle.project.auth.config.LoginAttemptProperties;
import com.aivle.project.auth.exception.AuthErrorCode;
import com.aivle.project.auth.exception.AuthException;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	private LoginAttemptService loginAttemptService;

	@BeforeEach
	void setUp() {
		LoginAttemptProperties properties = new LoginAttemptProperties();
		properties.setMaxFailures(5);
		properties.setFailureWindow(Duration.ofMinutes(15));
		properties.setLockDuration(Duration.ofMinutes(15));

		loginAttemptService = new LoginAttemptService(redisTemplate, properties);
	}

	@Test
	@DisplayName("첫 로그인 실패는 카운트만 증가시키고 잠금은 하지 않는다")
	void recordFailure_shouldIncreaseCountWithoutLockOnFirstFailure() {
		// given
		when(redisTemplate.execute(
			org.mockito.ArgumentMatchers.any(DefaultRedisScript.class),
			org.mockito.ArgumentMatchers.eq(java.util.Collections.singletonList("login:fail-count:user@example.com")),
			org.mockito.ArgumentMatchers.anyString()
		)).thenReturn(1L);

		// when
		boolean locked = loginAttemptService.recordFailure("user@example.com");

		// then
		assertThat(locked).isFalse();
	}

	@Test
	@DisplayName("최대 실패 횟수에 도달하면 잠금 키를 생성한다")
	void recordFailure_shouldLockWhenThresholdReached() {
		// given
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(redisTemplate.execute(
			org.mockito.ArgumentMatchers.any(DefaultRedisScript.class),
			org.mockito.ArgumentMatchers.eq(java.util.Collections.singletonList("login:fail-count:user@example.com")),
			org.mockito.ArgumentMatchers.anyString()
		)).thenReturn(5L);

		// when
		boolean locked = loginAttemptService.recordFailure("user@example.com");

		// then
		assertThat(locked).isTrue();
		verify(valueOperations).set("login:lock:user@example.com", "1", Duration.ofMinutes(15));
		verify(redisTemplate).delete("login:fail-count:user@example.com");
	}

	@Test
	@DisplayName("잠금 키가 있으면 로그인 시도 제한 예외를 반환한다")
	void validateNotLocked_shouldThrowWhenLocked() {
		// given
		when(redisTemplate.hasKey("login:lock:user@example.com")).thenReturn(true);

		// when & then
		assertThatThrownBy(() -> loginAttemptService.validateNotLocked("user@example.com"))
			.isInstanceOf(AuthException.class)
			.hasMessage(AuthErrorCode.LOGIN_ATTEMPT_LIMITED.getMessage());
	}

	@Test
	@DisplayName("로그인 성공 후 실패 카운트를 초기화한다")
	void clearFailures_shouldDeleteFailCountKey() {
		// when
		loginAttemptService.clearFailures("user@example.com");

		// then
		verify(redisTemplate).delete("login:fail-count:user@example.com");
	}
}
