package com.aivle.project.common.error;

import com.aivle.project.auth.exception.AuthException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 전역 예외 처리 핸들러.
 */
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

	private final Clock clock = Clock.systemUTC();

	@ExceptionHandler(AuthException.class)
	public ResponseEntity<ErrorResponse> handleAuthException(AuthException ex, HttpServletRequest request) {
		return buildResponse(ex.getErrorCode(), request.getRequestURI());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
		MethodArgumentNotValidException ex,
		HttpServletRequest request
	) {
		List<FieldErrorResponse> errors = ex.getBindingResult().getFieldErrors().stream()
			.map(error -> new FieldErrorResponse(error.getField(), error.getDefaultMessage()))
			.toList();
		return buildResponse(CommonErrorCode.COMMON_400_VALIDATION, request.getRequestURI(), errors);
	}

	@ExceptionHandler(BindException.class)
	public ResponseEntity<ErrorResponse> handleBindException(BindException ex, HttpServletRequest request) {
		List<FieldErrorResponse> errors = ex.getBindingResult().getFieldErrors().stream()
			.map(error -> new FieldErrorResponse(error.getField(), error.getDefaultMessage()))
			.toList();
		return buildResponse(CommonErrorCode.COMMON_400_VALIDATION, request.getRequestURI(), errors);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ErrorResponse> handleConstraintViolation(
		ConstraintViolationException ex,
		HttpServletRequest request
	) {
		List<FieldErrorResponse> errors = ex.getConstraintViolations().stream()
			.map(violation -> new FieldErrorResponse(violation.getPropertyPath().toString(), violation.getMessage()))
			.toList();
		return buildResponse(CommonErrorCode.COMMON_400_VALIDATION, request.getRequestURI(), errors);
	}

	@ExceptionHandler({
		IllegalArgumentException.class,
		MethodArgumentTypeMismatchException.class,
		MissingServletRequestParameterException.class,
		HttpMessageNotReadableException.class
	})
	public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex, HttpServletRequest request) {
		return buildResponse(CommonErrorCode.COMMON_400, request.getRequestURI());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
		return buildResponse(CommonErrorCode.COMMON_500, request.getRequestURI());
	}

	private ResponseEntity<ErrorResponse> buildResponse(ErrorCode code, String path) {
		String timestamp = OffsetDateTime.now(clock).toString();
		return ResponseEntity.status(code.getStatus()).body(ErrorResponse.of(code, path, timestamp));
	}

	private ResponseEntity<ErrorResponse> buildResponse(
		ErrorCode code,
		String path,
		List<FieldErrorResponse> errors
	) {
		String timestamp = OffsetDateTime.now(clock).toString();
		return ResponseEntity.status(code.getStatus()).body(ErrorResponse.of(code, path, timestamp, errors));
	}
}
