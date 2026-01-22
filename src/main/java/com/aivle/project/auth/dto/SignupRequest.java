package com.aivle.project.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 회원가입 요청 DTO.
 */
@Getter
@Setter
public class SignupRequest {

	@NotBlank
	@Email
	private String email;

	@NotBlank
	@Size(min = 8, max = 64)
	private String password;

	@NotBlank
	@Size(min = 2, max = 50)
	private String name;

	@Size(max = 20)
	private String phone;
}
