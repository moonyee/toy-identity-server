package com.platform.auth.identity.exception;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Getter;

/**
 * 비즈니스/플랫폼 에러 코드.
 * 각 코드는 고유 code, 사용자 메시지, 그리고 응답 HttpStatus를 함께 보유한다.
 * GlobalExceptionHandler가 status 필드를 그대로 응답에 사용하므로
 * 새 코드를 추가할 때 반드시 적절한 status를 지정해야 한다.
 */
@Getter
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum ErrorCode {

	// 공통
	TEMPORARY_SERVER_ERROR("GE00500", "오류가 발생하였습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
	INTERNAL_SERVER_ERROR("GE00501", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
	NOT_FOUND("GE00404", "요청한 리소스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
	FORBIDDEN("GE00403", "접근이 금지되었습니다.", HttpStatus.FORBIDDEN),
	ACCESS_DENIED("GE00403A", "권한이 없습니다.", HttpStatus.FORBIDDEN),
	UNAUTHORIZED("GE00401", "인증이 필요합니다.", HttpStatus.UNAUTHORIZED),

	// USER, LOGIN
	USER_NOT_FOUND("IE00001", "아이디가 틀렸거나 등록 되어있지 않습니다.", HttpStatus.UNAUTHORIZED),
	ACCESS_TOKEN_EXPIRED("IE00002", "토큰이 만료되었습니다.", HttpStatus.UNAUTHORIZED),
	MAX_SESSION_EXCEEDED("IE00003", "최대 동시 접속 수를 초과했습니다.", HttpStatus.CONFLICT);

	private final String code;
	private final String message;
	private final HttpStatus status;

	ErrorCode(String code, String message, HttpStatus status) {
		this.code = code;
		this.message = message;
		this.status = status;
	}

	/**
	 * 주어진 메시지와 동일한 텍스트를 가진 ErrorCode 탐색.
	 * 일치 항목이 없으면 INTERNAL_SERVER_ERROR를 반환한다.
	 */
	public static ErrorCode findByMessage(String msg) {
		for (ErrorCode errorCode : ErrorCode.values()) {
			if (errorCode.getMessage() != null && errorCode.getMessage().equals(msg)) {
				return errorCode;
			}
		}
		return INTERNAL_SERVER_ERROR;
	}
}
