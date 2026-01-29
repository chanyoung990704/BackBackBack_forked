package com.aivle.project.auth.service;

import com.aivle.project.auth.entity.RefreshTokenEntity;
import com.aivle.project.auth.exception.AuthErrorCode;
import com.aivle.project.auth.exception.AuthException;
import com.aivle.project.auth.repository.RefreshTokenRepository;
import com.aivle.project.auth.token.JwtTokenService;
import com.aivle.project.auth.token.RefreshTokenCache;
import com.aivle.project.user.security.CustomUserDetails;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Refresh Token 저장 및 검증 처리.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

	private static final String REFRESH_TOKEN_KEY = "refresh:%s";
	private static final String SESSION_KEY = "sessions:%s";
	private static final String DEFAULT_DEVICE_ID = "default";

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;
	private final RefreshTokenRepository refreshTokenRepository;
	private final JwtTokenService jwtTokenService;
	private final Clock clock = Clock.systemUTC();

	public RefreshTokenCache storeToken(
		CustomUserDetails userDetails,
		String refreshToken,
		String deviceId,
		String deviceInfo,
		String ipAddress
	) {
		String normalizedDeviceId = normalizeDeviceId(deviceId);
		long now = Instant.now(clock).getEpochSecond();
		long expiresAt = now + jwtTokenService.getRefreshTokenExpirationSeconds();

		RefreshTokenCache cache = new RefreshTokenCache(
			refreshToken,
			userDetails.getId(),
			normalizedDeviceId,
			deviceInfo,
			ipAddress,
			now,
			expiresAt,
			now
		);

		storeRedis(cache);
		storeSession(cache.userId(), refreshToken);
		storeEntity(cache);
		return cache;
	}

	public RefreshTokenCache rotateToken(String oldToken, String newToken) {
		RefreshTokenCache current = loadValidToken(oldToken);
		revokeRedis(oldToken, current.userId());
		revokeEntity(oldToken);

		long now = Instant.now(clock).getEpochSecond();
		long expiresAt = now + jwtTokenService.getRefreshTokenExpirationSeconds();
		RefreshTokenCache rotated = current.rotate(newToken, now, expiresAt);

		storeRedis(rotated);
		storeSession(rotated.userId(), newToken);
		storeEntity(rotated);
		return rotated;
	}

	public RefreshTokenCache loadValidToken(String refreshToken) {
		RefreshTokenCache cache = loadRedis(refreshToken).orElseGet(() -> loadFromDatabase(refreshToken));
		long now = Instant.now(clock).getEpochSecond();
		if (cache.expiresAt() <= now) {
			revokeRedis(refreshToken, cache.userId());
			throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
		}
		return cache;
	}

	public void revokeToken(String refreshToken) {
		RefreshTokenCache cache = loadValidToken(refreshToken);
		revokeRedis(refreshToken, cache.userId());
		revokeEntity(refreshToken);
	}

	private void storeRedis(RefreshTokenCache cache) {
		try {
			String json = objectMapper.writeValueAsString(cache);
			long ttl = cache.expiresAt() - Instant.now(clock).getEpochSecond();
			if (ttl <= 0) {
				return;
			}
			redisTemplate.opsForValue().set(redisKey(cache.token()), json, java.time.Duration.ofSeconds(ttl));
		} catch (JsonProcessingException ex) {
			throw new IllegalStateException("Refresh Token 캐시 직렬화에 실패했습니다", ex);
		}
	}

	private Optional<RefreshTokenCache> loadRedis(String refreshToken) {
		String json = redisTemplate.opsForValue().get(redisKey(refreshToken));
		if (json == null) {
			return Optional.empty();
		}
		try {
			return Optional.of(objectMapper.readValue(json, RefreshTokenCache.class));
		} catch (JsonProcessingException ex) {
			throw new IllegalStateException("Refresh Token 캐시 역직렬화에 실패했습니다", ex);
		}
	}

	private RefreshTokenCache loadFromDatabase(String refreshToken) {
		RefreshTokenEntity entity = refreshTokenRepository.findByTokenValue(refreshToken)
			.orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN));
		if (entity.isRevoked()) {
			throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
		}
		LocalDateTime expiresAt = entity.getExpiresAt();
		if (expiresAt.isBefore(LocalDateTime.now(clock))) {
			throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
		}

		long issuedAt = toEpochSeconds(entity.getCreatedAt());
		long expiresAtEpoch = toEpochSeconds(entity.getExpiresAt());

		RefreshTokenCache cache = new RefreshTokenCache(
			entity.getTokenValue(),
			entity.getUserId(),
			DEFAULT_DEVICE_ID,
			entity.getDeviceInfo(),
			entity.getIpAddress(),
			issuedAt,
			expiresAtEpoch,
			issuedAt
		);
		storeRedis(cache);
		storeSession(cache.userId(), cache.token());
		return cache;
	}

	private void revokeRedis(String refreshToken, Long userId) {
		redisTemplate.delete(redisKey(refreshToken));
		redisTemplate.opsForSet().remove(sessionKey(userId), refreshToken);
	}

	private void revokeEntity(String refreshToken) {
		refreshTokenRepository.findByTokenValue(refreshToken).ifPresent(entity -> {
			entity.revoke();
			refreshTokenRepository.save(entity);
		});
	}

	private void storeEntity(RefreshTokenCache cache) {
		LocalDateTime expiresAt = LocalDateTime.ofInstant(Instant.ofEpochSecond(cache.expiresAt()), ZoneOffset.UTC);
		RefreshTokenEntity entity = new RefreshTokenEntity(
			cache.userId(),
			cache.token(),
			cache.deviceInfo(),
			cache.ipAddress(),
			expiresAt
		);
		refreshTokenRepository.save(entity);
	}

	private void storeSession(Long userId, String refreshToken) {
		redisTemplate.opsForSet().add(sessionKey(userId), refreshToken);
	}

	private String redisKey(String refreshToken) {
		return String.format(REFRESH_TOKEN_KEY, refreshToken);
	}

	private String sessionKey(Long userId) {
		return String.format(SESSION_KEY, String.valueOf(userId));
	}

	private String normalizeDeviceId(String deviceId) {
		if (deviceId == null || deviceId.isBlank()) {
			return DEFAULT_DEVICE_ID;
		}
		return deviceId;
	}

	private long toEpochSeconds(LocalDateTime time) {
		return time.toInstant(ZoneOffset.UTC).getEpochSecond();
	}
}
