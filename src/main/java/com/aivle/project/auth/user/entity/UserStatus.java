package com.aivle.project.auth.user.entity;

/**
 * 사용자 상태 값.
 */
public enum UserStatus {
	PENDING,
	ACTIVE,
	INACTIVE,
	BANNED;

	/**
	 * 이메일 인증이 완료된 상태인지 확인.
	 */
	public boolean isVerified() {
		return this == ACTIVE;
	}
}
