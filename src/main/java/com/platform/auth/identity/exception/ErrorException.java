package com.platform.auth.identity.exception;

import lombok.Getter;

/**
 * 도메인/플랫폼 비즈니스 예외.
 * RuntimeException의 message에 ErrorCode의 사용자 메시지를 채워주어
 * 로깅과 클라이언트 응답 양쪽에서 일관된 메시지가 노출되도록 한다.
 */
@Getter
public class ErrorException extends RuntimeException {

	private final ErrorCode errorCode;

	public ErrorException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

	public ErrorException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}
}
