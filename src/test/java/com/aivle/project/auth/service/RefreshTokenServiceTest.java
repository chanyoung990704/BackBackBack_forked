package com.aivle.project.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aivle.project.auth.entity.RefreshTokenEntity;
import com.aivle.project.auth.repository.RefreshTokenRepository;
import com.aivle.project.auth.token.JwtTokenService;
import com.aivle.project.auth.token.RefreshTokenCache;
import com.aivle.project.user.security.CustomUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

	private static final String USER_ID = "user-uuid";
	private static final String EMAIL = "user@example.com";

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	@Mock
	private JwtTokenService jwtTokenService;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@Mock
	private SetOperations<String, String> setOperations;

	@Captor
	private ArgumentCaptor<String> valueCaptor;

	@Captor
	private ArgumentCaptor<Duration> durationCaptor;

	private RefreshTokenService refreshTokenService;

	@BeforeEach
	void setUp() {
		ObjectMapper objectMapper = new ObjectMapper();
		refreshTokenService = new RefreshTokenService(redisTemplate, objectMapper, refreshTokenRepository, jwtTokenService);
	}

	@Test
	@DisplayName("리프레시 토큰 저장 시 Redis와 DB에 모두 기록한다")
	void storeToken_shouldPersistToRedisAndDatabase() throws Exception {
		// given: 사용자 정보와 만료 시간 설정을 준비
		stubValueOperations();
		stubSetOperations();
		CustomUserDetails userDetails = mock(CustomUserDetails.class);
		when(userDetails.getUuid()).thenReturn(UUID.randomUUID());
		when(userDetails.getUsername()).thenReturn(EMAIL);
		when(jwtTokenService.getRefreshTokenExpirationSeconds()).thenReturn(600L);

		// when: 리프레시 토큰을 저장
		refreshTokenService.storeToken(userDetails, "rt-1", "device-1", "ios", "127.0.0.1");

		// then: Redis 저장과 DB 저장이 수행된다
		verify(valueOperations).set(eq("refresh:rt-1"), valueCaptor.capture(), durationCaptor.capture());
		RefreshTokenCache cache = new ObjectMapper().readValue(valueCaptor.getValue(), RefreshTokenCache.class);
		assertThat(cache.token()).isEqualTo("rt-1");
		assertThat(cache.email()).isEqualTo(EMAIL);
		assertThat(durationCaptor.getValue().getSeconds()).isPositive();

		ArgumentCaptor<RefreshTokenEntity> entityCaptor = ArgumentCaptor.forClass(RefreshTokenEntity.class);
		verify(refreshTokenRepository).save(entityCaptor.capture());
		assertThat(entityCaptor.getValue().getToken()).isEqualTo("rt-1");
		assertThat(entityCaptor.getValue().getEmail()).isEqualTo(EMAIL);
	}

	@Test
	@DisplayName("Redis 미스 시 DB에서 로드하고 캐시를 복구한다")
	void loadValidToken_shouldFallbackToDatabaseAndRehydrateCache() {
		// given: Redis 미스와 DB에 존재하는 토큰 상태를 준비
		stubValueOperations();
		stubSetOperations();
		LocalDateTime expiresAt = LocalDateTime.now().plusDays(1);
		RefreshTokenEntity entity = new RefreshTokenEntity("rt-2", USER_ID, EMAIL, "device-2", "android", "127.0.0.1", expiresAt);
		ReflectionTestUtils.setField(entity, "createdAt", LocalDateTime.now().minusMinutes(5));

		when(valueOperations.get("refresh:rt-2")).thenReturn(null);
		when(refreshTokenRepository.findByToken("rt-2")).thenReturn(Optional.of(entity));

		// when: 유효한 토큰을 조회
		RefreshTokenCache cache = refreshTokenService.loadValidToken("rt-2");

		// then: 캐시가 재구성되고 DB 업데이트가 수행된다
		assertThat(cache.token()).isEqualTo("rt-2");
		assertThat(cache.email()).isEqualTo(EMAIL);
		verify(valueOperations).set(eq("refresh:rt-2"), any(String.class), any(Duration.class));
		verify(setOperations).add("sessions:" + USER_ID, "rt-2");
		verify(refreshTokenRepository, atLeastOnce()).save(entity);
	}

	@Test
	@DisplayName("리프레시 회전 시 기존 토큰이 폐기되고 신규 토큰이 저장된다")
	void rotateToken_shouldRevokeOldAndStoreNew() throws Exception {
		// given: 기존 토큰이 Redis와 DB에 존재하는 상태를 준비
		stubValueOperations();
		stubSetOperations();
		RefreshTokenCache existing = new RefreshTokenCache(
			"rt-old",
			USER_ID,
			EMAIL,
			"device-3",
			"ios",
			"127.0.0.1",
			1L,
			System.currentTimeMillis() / 1000 + 600,
			1L
		);
		String json = new ObjectMapper().writeValueAsString(existing);
		when(valueOperations.get("refresh:rt-old")).thenReturn(json);
		when(jwtTokenService.getRefreshTokenExpirationSeconds()).thenReturn(600L);

		RefreshTokenEntity entity = new RefreshTokenEntity("rt-old", USER_ID, EMAIL, "device-3", "ios", "127.0.0.1", LocalDateTime.now().plusDays(1));
		when(refreshTokenRepository.findByToken("rt-old")).thenReturn(Optional.of(entity));

		// when: 리프레시 토큰을 회전
		refreshTokenService.rotateToken("rt-old", "rt-new");

		// then: 기존 토큰 삭제와 신규 토큰 저장이 수행된다
		verify(redisTemplate).delete("refresh:rt-old");
		verify(setOperations).remove("sessions:" + USER_ID, "rt-old");
		verify(valueOperations).set(eq("refresh:rt-new"), any(String.class), any(Duration.class));
		verify(refreshTokenRepository, atLeastOnce()).save(any(RefreshTokenEntity.class));
	}

	@Test
	@DisplayName("전체 로그아웃 시 사용자 리프레시 토큰을 모두 폐기한다")
	void revokeByUserId_shouldRevokeAllTokens() {
		// given: 사용자 토큰 목록을 준비
		stubSetOperations();
		RefreshTokenEntity entity1 = new RefreshTokenEntity(
			"rt-10",
			USER_ID,
			EMAIL,
			"device-1",
			"ios",
			"127.0.0.1",
			LocalDateTime.now().plusDays(1)
		);
		RefreshTokenEntity entity2 = new RefreshTokenEntity(
			"rt-11",
			USER_ID,
			EMAIL,
			"device-2",
			"android",
			"127.0.0.1",
			LocalDateTime.now().plusDays(1)
		);
		when(refreshTokenRepository.findAllByUserIdAndRevokedFalse(USER_ID)).thenReturn(List.of(entity1, entity2));

		// when: 전체 로그아웃을 수행
		refreshTokenService.revokeByUserId(USER_ID);

		// then: Redis/DB에서 토큰이 폐기되고 세션 키가 제거된다
		verify(redisTemplate).delete("refresh:rt-10");
		verify(redisTemplate).delete("refresh:rt-11");
		verify(setOperations).remove("sessions:" + USER_ID, "rt-10");
		verify(setOperations).remove("sessions:" + USER_ID, "rt-11");
		verify(redisTemplate).delete("sessions:" + USER_ID);
		verify(refreshTokenRepository, atLeastOnce()).save(any(RefreshTokenEntity.class));
	}

	@Test
	@DisplayName("디바이스 로그아웃 시 해당 디바이스 토큰만 폐기한다")
	void revokeByUserIdAndDeviceId_shouldRevokeDeviceTokens() {
		// given: 특정 디바이스 토큰을 준비
		stubSetOperations();
		RefreshTokenEntity entity = new RefreshTokenEntity(
			"rt-20",
			USER_ID,
			EMAIL,
			"device-3",
			"ios",
			"127.0.0.1",
			LocalDateTime.now().plusDays(1)
		);
		when(refreshTokenRepository.findAllByUserIdAndDeviceIdAndRevokedFalse(USER_ID, "device-3"))
			.thenReturn(List.of(entity));

		// when: 디바이스 로그아웃을 수행
		refreshTokenService.revokeByUserIdAndDeviceId(USER_ID, "device-3");

		// then: 해당 토큰만 폐기된다
		verify(redisTemplate).delete("refresh:rt-20");
		verify(setOperations).remove("sessions:" + USER_ID, "rt-20");
		verify(redisTemplate, never()).delete("sessions:" + USER_ID);
		verify(refreshTokenRepository, atLeastOnce()).save(entity);
	}

	private void stubValueOperations() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
	}

	private void stubSetOperations() {
		when(redisTemplate.opsForSet()).thenReturn(setOperations);
	}
}
