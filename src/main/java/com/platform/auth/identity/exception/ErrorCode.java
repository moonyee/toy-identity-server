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
	MAX_SESSION_EXCEEDED("IE00003", "최대 동시 접속 수를 초과했습니다.", HttpStatus.CONFLICT),

	// JOIN, EMAIL VERIFICATION
	USER_ALREADY_EXISTS("IE00004", "이미 존재하는 아이디입니다.", HttpStatus.CONFLICT),
	EMAIL_ALREADY_EXISTS("IE00005", "이미 등록된 이메일입니다.", HttpStatus.CONFLICT),
	VERIFICATION_CODE_EXPIRED("IE00006", "인증 코드가 만료되었습니다. 다시 요청해 주세요.", HttpStatus.UNAUTHORIZED),
	VERIFICATION_CODE_INVALID("IE00007", "인증 코드가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),
	EMAIL_NOT_VERIFIED("IE00008", "이메일 인증이 필요합니다.", HttpStatus.FORBIDDEN),
	MAIL_ALREADY_VERIFIED("IE00009", "이미 인증된 계정입니다.", HttpStatus.CONFLICT),
	VERIFICATION_MAIL_SEND_FAILED("IE00010", "인증 메일 발송에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

	// WITHDRAW
	// 회원 탈퇴 전용. 탈퇴 엔드포인트에서 비밀번호 재확인 실패 시 사용한다.
	// USER_NOT_FOUND 와 분리된 이유: 탈퇴는 이미 인증된 사용자가 수행하므로 계정 존재 은폐가 불필요하고,
	// 비밀번호 오류임을 명확히 하여 UX·감사 로그 구분에 도움이 된다.
	PASSWORD_MISMATCH("IE00011", "비밀번호가 일치하지 않습니다.", HttpStatus.UNAUTHORIZED);

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
