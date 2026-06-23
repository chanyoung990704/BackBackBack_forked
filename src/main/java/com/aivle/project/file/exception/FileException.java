package com.aivle.project.file.exception;

import com.aivle.project.common.error.CommonException;

/**
 * 파일 업로드 예외.
 */
public class FileException extends CommonException {

	public FileException(FileErrorCode errorCode) {
		super(errorCode);
	}

	@Override
	public FileErrorCode getErrorCode() {
		return (FileErrorCode) super.getErrorCode();
	}
}
