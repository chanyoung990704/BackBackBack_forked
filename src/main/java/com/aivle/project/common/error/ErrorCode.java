package com.aivle.project.common.error;

import org.springframework.http.HttpStatus;

/**
 * 전역 에러 코드 인터페이스.
 */
public interface ErrorCode {

	String getCode();

	String getMessage();

	HttpStatus getStatus();
}
