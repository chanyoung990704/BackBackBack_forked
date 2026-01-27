package com.aivle.project.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 회원가입 요청 DTO.
 */
@Schema(description = "회원가입 요청")
@Getter
@Setter
public class SignupRequest {

	@NotBlank
	@Email
	@Schema(description = "이메일", example = "user@example.com")
	private String email;

	@NotBlank
	@Size(min = 8, max = 64)
	@Pattern(
		regexp = "^(?!.*(.)\\1\\1)(?:(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[^a-zA-Z0-9]).{8,}|(?:(?=.*[a-zA-Z])(?=.*[0-9])|(?=.*[a-zA-Z])(?=.*[^a-zA-Z0-9])|(?=.*[0-9])(?=.*[^a-zA-Z0-9])).{10,})$",
		message = "비밀번호는 영문/숫자/특수문자 중 3가지 포함(8자 이상) 또는 2가지 포함(10자 이상)이어야 하며, 3회 이상 연속된 문자를 사용할 수 없습니다."
	)
	@Schema(description = "비밀번호(정책: 영문/숫자/특수문자 조합)", example = "P@ssw0rd!")
	private String password;

	@NotBlank
	@Size(min = 2, max = 50)
	@Schema(description = "이름", example = "홍길동")
	private String name;

	@Size(max = 20)
	@Schema(description = "전화번호", example = "01012345678")
	private String phone;

	@NotBlank(message = "보안 검증이 필요합니다")
	@Schema(description = "Cloudflare Turnstile 토큰", example = "0.abc123...")
	private String turnstileToken;

	@JsonIgnore
	@AssertTrue(message = "비밀번호에 전화번호를 포함할 수 없습니다.")
	public boolean isPasswordValid() {
		if (password == null) {
			return true; // @NotBlank 처리
		}

		// 전화번호 포함 여부 확인
		if (phone != null && !phone.isBlank() && password.contains(phone)) {
			return false;
		}

		// TODO: 생년월일 포함 여부 확인 (구현 예정)
		// if (birthDate != null && password.contains(birthDate)) { return false; }

		return true;
	}
}
