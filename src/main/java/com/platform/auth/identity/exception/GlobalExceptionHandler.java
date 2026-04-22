package com.platform.auth.identity.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.platform.auth.identity.common.response.ApiResponseEntity;

import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	@ExceptionHandler(ErrorException.class)
	protected ResponseEntity<ApiResponseEntity<ErrorResponseData>> handleErrorException(ErrorException e) {
		ErrorCode errorCode = e.getErrorCode();
		log.warn("### Business Exception [{}] {}", errorCode != null ? errorCode.name() : "UNKNOWN", e.getMessage());

		ErrorResponseData errorData = ErrorResponseData.of(errorCode);
		if (e.getMessage() != null && !e.getMessage().isEmpty()) {
			errorData.setMessage(e.getMessage());
		}

		HttpStatus status = errorCode != null && errorCode.getStatus() != null
			? errorCode.getStatus()
			: HttpStatus.INTERNAL_SERVER_ERROR;

		return ResponseEntity.status(status).body(ApiResponseEntity.error(errorData, errorData.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	protected ResponseEntity<ApiResponseEntity<ErrorResponseData>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
		String errorMessage = e.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
		log.warn("### Validation Failed: {}", errorMessage);

		ErrorResponseData errorData = ErrorResponseData.of(ErrorCode.TEMPORARY_SERVER_ERROR);
		errorData.setMessage(errorMessage);

		return ResponseEntity.badRequest().body(ApiResponseEntity.error(errorData, errorMessage));
	}

	@ExceptionHandler(AccessDeniedException.class)
	protected ResponseEntity<ApiResponseEntity<ErrorResponseData>> handleAccessDeniedException(AccessDeniedException e) {
		ErrorResponseData errorData = ErrorResponseData.of(ErrorCode.ACCESS_DENIED);
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponseEntity.error(errorData, e.getMessage()));
	}

	@ExceptionHandler(JwtException.class)
	protected ResponseEntity<ApiResponseEntity<ErrorResponseData>> handleJwtException(JwtException e) {
		log.warn("### JWT Exception in Controller Layer: {}", e.getMessage());
		ErrorResponseData errorData = ErrorResponseData.of(ErrorCode.UNAUTHORIZED);
		errorData.setMessage(e.getMessage());
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
			.body(ApiResponseEntity.error(errorData, e.getMessage()));
	}

	@ExceptionHandler(Exception.class)
	protected ResponseEntity<ApiResponseEntity<ErrorResponseData>> handleGeneralException(Exception e) {
		log.error("### Unexpected Exception: ", e);

		ErrorResponseData errorData = ErrorResponseData.of(ErrorCode.INTERNAL_SERVER_ERROR);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(ApiResponseEntity.error(errorData, "서버 내부 오류가 발생했습니다."));
	}
}
