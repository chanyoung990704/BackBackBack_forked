package com.aivle.project.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 로그인 요청 DTO.
 */
@Getter
@Setter
public class LoginRequest {

	@NotBlank
	@Email
	private String email;

	@NotBlank
	private String password;

	private String deviceId;

	private String deviceInfo;
}
