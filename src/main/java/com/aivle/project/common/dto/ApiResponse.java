package com.aivle.project.common.dto;

import com.aivle.project.common.error.ErrorResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * 공통 API 응답 포맷.
 */
@Schema(description = "공통 API 응답")
public record ApiResponse<T>(
	@Schema(description = "성공 여부", example = "true")
	boolean success,
	@Schema(description = "응답 데이터")
	T data,
	@Schema(description = "에러 정보")
	ErrorResponse error,
	@Schema(description = "응답 생성 시각(UTC, ISO-8601)", example = "2026-01-25T12:34:56Z")
	String timestamp
) {

	public static <T> ApiResponse<T> ok(T data) {
		return new ApiResponse<>(true, data, null, OffsetDateTime.now(ZoneOffset.UTC).toString());
	}

	public static ApiResponse<Void> ok() {
		return new ApiResponse<>(true, null, null, OffsetDateTime.now(ZoneOffset.UTC).toString());
	}

	public static <T> ApiResponse<T> fail(ErrorResponse errorResponse) {
		return new ApiResponse<>(false, null, errorResponse, errorResponse.timestamp());
	}
}
