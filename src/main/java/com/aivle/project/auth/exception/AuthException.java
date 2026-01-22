package com.aivle.project.auth.exception;

/**
 * 인증 관련 런타임 예외.
 */
public class AuthException extends RuntimeException {

	private final AuthErrorCode errorCode;

	public AuthException(AuthErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

	public AuthErrorCode getErrorCode() {
		return errorCode;
	}
}
