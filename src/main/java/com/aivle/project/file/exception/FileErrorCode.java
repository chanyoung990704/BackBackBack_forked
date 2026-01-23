package com.aivle.project.file.exception;

import com.aivle.project.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * 파일 업로드 에러 코드.
 */
public enum FileErrorCode implements ErrorCode {
	FILE_400_EMPTY("FILE_400_EMPTY", "파일이 비어있습니다.", HttpStatus.BAD_REQUEST),
	FILE_400_SIZE("FILE_400_SIZE", "파일 크기가 허용 범위를 초과했습니다.", HttpStatus.PAYLOAD_TOO_LARGE),
	FILE_400_TOTAL_SIZE("FILE_400_TOTAL_SIZE", "전체 파일 크기가 허용 범위를 초과했습니다.", HttpStatus.PAYLOAD_TOO_LARGE),
	FILE_400_TOO_MANY("FILE_400_TOO_MANY", "업로드 가능한 파일 개수를 초과했습니다.", HttpStatus.BAD_REQUEST),
	FILE_400_EXTENSION("FILE_400_EXTENSION", "허용되지 않은 파일 확장자입니다.", HttpStatus.BAD_REQUEST),
	FILE_400_CONTENT_TYPE("FILE_400_CONTENT_TYPE", "허용되지 않은 파일 타입입니다.", HttpStatus.BAD_REQUEST),
	FILE_400_SIGNATURE("FILE_400_SIGNATURE", "파일 시그니처가 일치하지 않습니다.", HttpStatus.BAD_REQUEST),
	FILE_404_NOT_FOUND("FILE_404_NOT_FOUND", "파일을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
	FILE_500_SIGNATURE("FILE_500_SIGNATURE", "파일 시그니처 검증 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
	FILE_500_STORAGE("FILE_500_STORAGE", "파일 저장에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

	private final String code;
	private final String message;
	private final HttpStatus status;

	FileErrorCode(String code, String message, HttpStatus status) {
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
