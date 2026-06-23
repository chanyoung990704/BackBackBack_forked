package com.aivle.project.auth.exception;

import com.aivle.project.common.error.CommonException;

/**
 * 인증 관련 런타임 예외.
 */
public class AuthException extends CommonException {

	public AuthException(AuthErrorCode errorCode) {
		super(errorCode);
	}

	@Override
	public AuthErrorCode getErrorCode() {
		return (AuthErrorCode) super.getErrorCode();
	}
}
