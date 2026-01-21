package com.aivle.project.common.error;

/**
 * 필드 단위 검증 에러 응답.
 */
public record FieldErrorResponse(String field, String message) {
}
