package com.aivle.project.common.error;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * 공통 에러 응답 포맷.
 */
public record ErrorResponse(
	String code,
	String message,
	String timestamp,
	String path,
	List<FieldErrorResponse> errors
) {

	public static ErrorResponse of(String code, String message, String path) {
		String timestamp = OffsetDateTime.now(ZoneOffset.UTC).toString();
		return new ErrorResponse(code, message, timestamp, path, null);
	}

	public static ErrorResponse of(ErrorCode code, String path, String timestamp) {
		return new ErrorResponse(code.getCode(), code.getMessage(), timestamp, path, null);
	}

	public static ErrorResponse of(ErrorCode code, String path, String timestamp, List<FieldErrorResponse> errors) {
		return new ErrorResponse(code.getCode(), code.getMessage(), timestamp, path, errors);
	}
}
