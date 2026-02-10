package com.aivle.project.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 관리자 사용자 목록 응답 DTO.
 */
@Schema(description = "관리자 사용자 목록 아이템")
public record AdminUserListItemDto(
	@Schema(description = "사용자 ID", example = "1")
	Long id,
	@Schema(description = "이름", example = "홍길동")
	String name,
	@Schema(description = "이메일", example = "user@company.com")
	String email
) {
}
