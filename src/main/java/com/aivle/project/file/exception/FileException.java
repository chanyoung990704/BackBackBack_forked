package com.aivle.project.file.exception;

/**
 * 파일 업로드 예외.
 */
public class FileException extends RuntimeException {

	private final FileErrorCode errorCode;

	public FileException(FileErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

	public FileErrorCode getErrorCode() {
		return errorCode;
	}
}
