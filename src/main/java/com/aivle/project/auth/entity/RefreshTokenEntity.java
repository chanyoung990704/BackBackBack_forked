package com.aivle.project.auth.entity;

import com.aivle.project.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * refresh_tokens 테이블 엔티티.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenEntity extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "token", nullable = false, length = 500, unique = true)
	private String token;

	@Column(name = "user_id", nullable = false, length = 36)
	private String userId;

	@Column(name = "email", nullable = false, length = 100)
	private String email;

	@Column(name = "device_id", nullable = false, length = 100)
	private String deviceId;

	@Column(name = "device_info", length = 200)
	private String deviceInfo;

	@Column(name = "ip_address", length = 45)
	private String ipAddress;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	@Column(name = "revoked", nullable = false)
	private boolean revoked;

	@Column(name = "revoked_at")
	private LocalDateTime revokedAt;

	@Column(name = "last_used_at")
	private LocalDateTime lastUsedAt;

	public RefreshTokenEntity(
		String token,
		String userId,
		String email,
		String deviceId,
		String deviceInfo,
		String ipAddress,
		LocalDateTime expiresAt
	) {
		this.token = token;
		this.userId = userId;
		this.email = email;
		this.deviceId = deviceId;
		this.deviceInfo = deviceInfo;
		this.ipAddress = ipAddress;
		this.expiresAt = expiresAt;
		this.revoked = false;
		this.lastUsedAt = LocalDateTime.now();
	}

	public void revoke() {
		if (revoked) {
			return;
		}
		revoked = true;
		revokedAt = LocalDateTime.now();
	}

	public void updateLastUsedAt() {
		lastUsedAt = LocalDateTime.now();
	}
}
