package com.aivle.project.common.dto;

import com.aivle.project.common.error.ErrorResponse;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * 공통 API 응답 포맷.
 */
public record ApiResponse<T>(
	boolean success,
	T data,
	ErrorResponse error,
	String timestamp
) {

	public static <T> ApiResponse<T> ok(T data) {
		return new ApiResponse<>(true, data, null, OffsetDateTime.now(ZoneOffset.UTC).toString());
	}

	public static ApiResponse<Void> ok() {
		return new ApiResponse<>(true, null, null, OffsetDateTime.now(ZoneOffset.UTC).toString());
	}

	public static ApiResponse<Void> fail(ErrorResponse errorResponse) {
		return new ApiResponse<>(false, null, errorResponse, errorResponse.timestamp());
	}
}