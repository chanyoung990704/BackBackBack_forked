package com.aivle.project.auth.dto;

import com.aivle.project.user.entity.RoleName;
import com.aivle.project.user.entity.UserEntity;
import com.aivle.project.user.entity.UserStatus;
import java.util.UUID;

/**
 * 회원가입 응답 DTO.
 */
public record SignupResponse(
	Long id,
	UUID uuid,
	String email,
	UserStatus status,
	RoleName role
) {
	public static SignupResponse of(UserEntity user, RoleName role) {
		return new SignupResponse(user.getId(), user.getUuid(), user.getEmail(), user.getStatus(), role);
	}
}
