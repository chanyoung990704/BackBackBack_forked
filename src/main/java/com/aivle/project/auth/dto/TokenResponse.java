package com.aivle.project.auth.dto;

/**
 * 토큰 응답 DTO.
 */
public record TokenResponse(
	String tokenType,
	String accessToken,
	long expiresIn,
	String refreshToken,
	long refreshExpiresIn
) {
	public static TokenResponse of(String accessToken, long accessExpiresIn, String refreshToken, long refreshExpiresIn) {
		return new TokenResponse("Bearer", accessToken, accessExpiresIn, refreshToken, refreshExpiresIn);
	}
}
