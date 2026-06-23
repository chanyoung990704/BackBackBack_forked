package com.aivle.project.auth.dto;

import com.aivle.project.auth.user.dto.UserSummaryDto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 로그인 성공 응답 DTO (FE 명세서와 일치)
 */
@Schema(description = "로그인 응답")
public record AuthLoginResponse(
	@Schema(description = "토큰 타입", example = "Bearer")
	String tokenType,

	@Schema(description = "액세스 토큰", example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...")
	String accessToken,

	@Schema(description = "액세스 토큰 만료(초)", example = "1800")
	long expiresIn,

	@JsonIgnore
	@Schema(description = "리프레시 토큰 (쿠키로 전달됨)", hidden = true)
	String refreshToken,

	@JsonIgnore
	@Schema(description = "리프레시 토큰 만료(초)", hidden = true)
	long refreshExpiresIn,

	@Schema(description = "비밀번호 만료 여부", example = "false")
	boolean passwordExpired,

	@Schema(description = "사용자 정보")
	UserSummaryDto user
) {
	public static AuthLoginResponse of(TokenResponse token, UserSummaryDto user) {
		return new AuthLoginResponse(
			token.tokenType(),
			token.accessToken(),
			token.expiresIn(),
			token.refreshToken(),
			token.refreshExpiresIn(),
			token.passwordExpired(),
			user
		);
	}
}