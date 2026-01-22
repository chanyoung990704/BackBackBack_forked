package com.aivle.project.auth.exception;

import com.aivle.project.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * 인증/인가 에러 코드.
 */
public enum AuthErrorCode implements ErrorCode {
	INVALID_CREDENTIALS("INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),
	INVALID_REFRESH_TOKEN("AUTH_401", "인증에 실패했습니다. (토큰 만료 또는 유효하지 않음)", HttpStatus.UNAUTHORIZED),
	INVALID_ACCESS_TOKEN("AUTH_401", "인증에 실패했습니다. (토큰 만료 또는 유효하지 않음)", HttpStatus.UNAUTHORIZED),
	EMAIL_ALREADY_EXISTS("AUTH_409", "이미 가입된 이메일입니다.", HttpStatus.CONFLICT);

	private final String code;
	private final String message;
	private final HttpStatus status;

	AuthErrorCode(String code, String message, HttpStatus status) {
		this.code = code;
		this.message = message;
		this.status = status;
	}

	@Override
	public String getCode() {
		return code;
	}

	@Override
	public String getMessage() {
		return message;
	}

	@Override
	public HttpStatus getStatus() {
		return status;
	}
}
