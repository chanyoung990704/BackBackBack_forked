package com.aivle.project.auth.dto;

import com.aivle.project.auth.user.entity.RoleName;
import com.aivle.project.auth.user.entity.UserEntity;
import com.aivle.project.auth.user.entity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/**
 * 회원가입 응답 DTO.
 */
@Schema(description = "회원가입 응답")
public record SignupResponse(
	@Schema(description = "사용자 ID", example = "1")
	Long id,
	@Schema(description = "사용자 UUID", example = "b0f3c5b8-7e2f-4d3b-9a3a-0b2a3f4c5d6e")
	UUID uuid,
	@Schema(description = "이메일", example = "user@example.com")
	String email,
	@Schema(description = "상태", example = "ACTIVE")
	UserStatus status,
	@Schema(description = "역할", example = "USER")
	RoleName role
) {
}
