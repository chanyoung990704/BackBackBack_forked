package com.aivle.project.auth.token;

/**
 * Redis에 저장되는 Refresh Token 메타데이터.
 */
public record RefreshTokenCache(
	String token,
	String userId,
	String email,
	String deviceId,
	String deviceInfo,
	String ipAddress,
	long issuedAt,
	long expiresAt,
	long lastUsedAt
) {
	public RefreshTokenCache rotate(String newToken, long nowEpochSeconds, long newExpiresAt) {
		return new RefreshTokenCache(
			newToken,
			userId,
			email,
			deviceId,
			deviceInfo,
			ipAddress,
			nowEpochSeconds,
			newExpiresAt,
			nowEpochSeconds
		);
	}
}
