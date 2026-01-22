package com.aivle.project.common.error;

/**
 * 공통 에러 코드 기반 예외.
 */
public class CommonException extends RuntimeException {

	private final ErrorCode errorCode;

	public CommonException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

	public ErrorCode getErrorCode() {
		return errorCode;
	}
}
