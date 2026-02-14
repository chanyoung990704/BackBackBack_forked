package com.aivle.project.auth.service;

import com.aivle.project.auth.entity.RefreshTokenEntity;
import com.aivle.project.auth.exception.AuthErrorCode;
import com.aivle.project.auth.exception.AuthException;
import com.aivle.project.auth.repository.RefreshTokenRepository;
import com.aivle.project.auth.token.JwtTokenService;
import com.aivle.project.auth.token.RefreshTokenCache;
import com.aivle.project.common.security.TokenHashService;
import com.aivle.project.user.security.CustomUserDetails;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Refresh Token 저장 및 검증 처리.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefreshTokenService {

	private static final String REFRESH_TOKEN_KEY = "refresh:%s";
	private static final String SESSION_KEY = "sessions:%s";
	private static final String DEFAULT_DEVICE_ID = "default";

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;
	private final RefreshTokenRepository refreshTokenRepository;
	private final JwtTokenService jwtTokenService;
	private final TokenHashService tokenHashService;
	private final Clock clock = Clock.systemUTC();

	@Transactional
	public RefreshTokenCache storeToken(
		CustomUserDetails userDetails,
		String refreshToken,
		String deviceId,
		String deviceInfo,
		String ipAddress
	) {
		String normalizedDeviceId = normalizeDeviceId(deviceId);
		String tokenHash = tokenHashService.hash(refreshToken);
		long now = Instant.now(clock).toEpochMilli();
		long expiresAt = now + (jwtTokenService.getRefreshTokenExpirationSeconds() * 1000);

		RefreshTokenCache cache = new RefreshTokenCache(
			tokenHash,
			userDetails.getId(),
			normalizedDeviceId,
			deviceInfo,
			ipAddress,
			now,
			expiresAt,
			now
		);

		storeRedis(cache);
		storeSession(cache.userId(), tokenHash);
		storeEntity(cache);
		return cache;
	}

	@Transactional
	public RefreshTokenCache rotateToken(String oldToken, String newToken) {
		RefreshTokenCache current = loadValidToken(oldToken);
		revokeRedis(oldToken, current.userId());
		revokeEntity(oldToken);

		long now = Instant.now(clock).toEpochMilli();
		long expiresAt = now + (jwtTokenService.getRefreshTokenExpirationSeconds() * 1000);
		String newTokenHash = tokenHashService.hash(newToken);
		RefreshTokenCache rotated = current.rotate(newTokenHash, now, expiresAt);

		storeRedis(rotated);
		storeSession(rotated.userId(), newTokenHash);
		storeEntity(rotated);
		return rotated;
	}

	public RefreshTokenCache loadValidToken(String refreshToken) {
		String tokenHash = tokenHashService.hash(refreshToken);
		String legacyTokenHash = tokenHashService.legacyHash(refreshToken);
		RefreshTokenCache cache = loadRedis(tokenHash)
			.or(() -> loadRedis(legacyTokenHash).map(legacy -> migrateLegacyCache(legacy, legacyTokenHash, tokenHash)))
			.or(() -> loadLegacyRedis(refreshToken).map(legacy -> migrateLegacyCache(legacy, refreshToken, tokenHash)))
			.orElseGet(() -> loadFromDatabase(refreshToken, tokenHash, legacyTokenHash));
		long now = Instant.now(clock).toEpochMilli();
		long expiresAt = normalizeEpochMillis(cache.expiresAt());
		if (expiresAt <= now) {
			revokeRedis(refreshToken, cache.userId());
			throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
		}
		return cache;
	}

	@Transactional
	public void revokeToken(String refreshToken) {
		RefreshTokenCache cache = loadValidToken(refreshToken);
		revokeRedis(refreshToken, cache.userId());
		revokeEntity(refreshToken);
	}

	@Transactional
	public void revokeAllByUserId(Long userId) {
		if (userId == null) {
			return;
		}

		String key = sessionKey(userId);
		Set<String> tokens = redisTemplate.opsForSet().members(key);
		if (tokens != null) {
			for (String token : tokens) {
				redisTemplate.delete(redisKey(token));
			}
		}
		redisTemplate.delete(key);

		List<RefreshTokenEntity> activeTokens = refreshTokenRepository.findAllByUserIdAndRevokedFalse(userId);
		for (RefreshTokenEntity entity : activeTokens) {
			entity.revoke();
		}
		if (!activeTokens.isEmpty()) {
			refreshTokenRepository.saveAll(activeTokens);
		}
	}

	private void storeRedis(RefreshTokenCache cache) {
		try {
			String json = objectMapper.writeValueAsString(cache);
			long expiresAtMillis = normalizeEpochMillis(cache.expiresAt());
			long ttlMillis = expiresAtMillis - Instant.now(clock).toEpochMilli();
			if (ttlMillis <= 0) {
				return;
			}
			redisTemplate.opsForValue().set(redisKey(cache.token()), json, java.time.Duration.ofMillis(ttlMillis));
		} catch (JsonProcessingException ex) {
			throw new IllegalStateException("Refresh Token 캐시 직렬화에 실패했습니다", ex);
		}
	}

	private Optional<RefreshTokenCache> loadRedis(String tokenIdentifier) {
		String json = redisTemplate.opsForValue().get(redisKey(tokenIdentifier));
		if (json == null) {
			return Optional.empty();
		}
		try {
			return Optional.of(objectMapper.readValue(json, RefreshTokenCache.class));
		} catch (JsonProcessingException ex) {
			throw new IllegalStateException("Refresh Token 캐시 역직렬화에 실패했습니다", ex);
		}
	}

	private Optional<RefreshTokenCache> loadLegacyRedis(String refreshToken) {
		return loadRedis(refreshToken);
	}

	private RefreshTokenCache migrateLegacyCache(RefreshTokenCache legacyCache, String legacyIdentifier, String tokenHash) {
		RefreshTokenCache migratedCache = new RefreshTokenCache(
			tokenHash,
			legacyCache.userId(),
			legacyCache.deviceId(),
			legacyCache.deviceInfo(),
			legacyCache.ipAddress(),
			legacyCache.issuedAt(),
			legacyCache.expiresAt(),
			legacyCache.lastUsedAt()
		);
		storeRedis(migratedCache);
		storeSession(migratedCache.userId(), tokenHash);
		redisTemplate.delete(redisKey(legacyIdentifier));
		redisTemplate.opsForSet().remove(sessionKey(migratedCache.userId()), legacyIdentifier);
		return migratedCache;
	}

	private RefreshTokenCache loadFromDatabase(String refreshToken, String tokenHash, String legacyTokenHash) {
		RefreshTokenEntity entity = refreshTokenRepository.findByTokenHash(tokenHash)
			.or(() -> refreshTokenRepository.findByTokenHash(legacyTokenHash))
			.or(() -> refreshTokenRepository.findByTokenValue(refreshToken))
			.orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN));
		if (entity.isRevoked()) {
			throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
		}
		LocalDateTime expiresAt = entity.getExpiresAt();
		if (expiresAt.isBefore(LocalDateTime.now(clock))) {
			throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
		}
		if (entity.getTokenHash() == null || tokenHashService.isLegacyHash(refreshToken, entity.getTokenHash())) {
			entity.migrateToHashed(tokenHash);
			refreshTokenRepository.save(entity);
		}

		long issuedAt = toEpochMillis(entity.getCreatedAt());
		long expiresAtEpoch = toEpochMillis(entity.getExpiresAt());

		RefreshTokenCache cache = new RefreshTokenCache(
			tokenHash,
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
		String tokenHash = tokenHashService.hash(refreshToken);
		String legacyTokenHash = tokenHashService.legacyHash(refreshToken);
		redisTemplate.delete(redisKey(tokenHash));
		redisTemplate.delete(redisKey(legacyTokenHash));
		redisTemplate.delete(redisKey(refreshToken));
		redisTemplate.opsForSet().remove(sessionKey(userId), tokenHash);
		redisTemplate.opsForSet().remove(sessionKey(userId), legacyTokenHash);
		redisTemplate.opsForSet().remove(sessionKey(userId), refreshToken);
	}

	private void revokeEntity(String refreshToken) {
		String tokenHash = tokenHashService.hash(refreshToken);
		String legacyTokenHash = tokenHashService.legacyHash(refreshToken);
		refreshTokenRepository.findByTokenHash(tokenHash)
			.or(() -> refreshTokenRepository.findByTokenHash(legacyTokenHash))
			.or(() -> refreshTokenRepository.findByTokenValue(refreshToken))
			.ifPresent(entity -> {
				entity.revoke();
				refreshTokenRepository.save(entity);
			});
	}

	private void storeEntity(RefreshTokenCache cache) {
		long expiresAtEpochMillis = normalizeEpochMillis(cache.expiresAt());
		LocalDateTime expiresAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(expiresAtEpochMillis), ZoneOffset.UTC);
		RefreshTokenEntity entity = RefreshTokenEntity.hashed(
			cache.userId(),
			cache.token(),
			cache.deviceInfo(),
			cache.ipAddress(),
			expiresAt
		);
		refreshTokenRepository.save(entity);
	}

	private void storeSession(Long userId, String tokenIdentifier) {
		redisTemplate.opsForSet().add(sessionKey(userId), tokenIdentifier);
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

	private long toEpochMillis(LocalDateTime time) {
		return time.toInstant(ZoneOffset.UTC).toEpochMilli();
	}

	private long normalizeEpochMillis(long epochValue) {
		// 레거시(초 단위) 캐시와 신규(밀리초 단위) 캐시를 모두 허용한다.
		if (epochValue > 0 && epochValue < 10_000_000_000L) {
			return epochValue * 1000;
		}
		return epochValue;
	}
}
