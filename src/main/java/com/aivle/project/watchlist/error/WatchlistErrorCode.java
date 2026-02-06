package com.aivle.project.watchlist.error;

import com.aivle.project.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * 위시리스트 도메인 에러 코드.
 */
public enum WatchlistErrorCode implements ErrorCode {

	WATCHLIST_DUPLICATE("WATCHLIST_DUPLICATE", "중복저장입니다.", HttpStatus.CONFLICT),
	WATCHLIST_NOT_FOUND("WATCHLIST_NOT_FOUND", "watchlist가 존재하지 않습니다.", HttpStatus.NOT_FOUND),
	WATCHLIST_FORBIDDEN("WATCHLIST_FORBIDDEN", "다른 사용자의 watchlist는 삭제할 수 없습니다.", HttpStatus.FORBIDDEN);

	private final String code;
	private final String message;
	private final HttpStatus status;

	WatchlistErrorCode(String code, String message, HttpStatus status) {
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
