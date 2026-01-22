package com.aivle.project.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 토큰 재발급 요청 DTO.
 */
@Getter
@Setter
public class TokenRefreshRequest {

	@NotBlank
	private String refreshToken;
}
