package com.aivle.project.common.error;

import org.springframework.http.HttpStatus;

/**
 * 공통 에러 코드 정의.
 */
public enum CommonErrorCode implements ErrorCode {
	COMMON_400("COMMON_400", "요청이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
	COMMON_400_VALIDATION("COMMON_400_VALIDATION", "입력값이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
	COMMON_404("COMMON_404", "리소스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
	COMMON_409("COMMON_409", "요청이 현재 상태와 충돌합니다.", HttpStatus.CONFLICT),
	COMMON_500("COMMON_500", "서버 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

	private final String code;
	private final String message;
	private final HttpStatus status;

	CommonErrorCode(String code, String message, HttpStatus status) {
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
