package com.platform.auth.identity.exception;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum ErrorCode {

    TEMPORARY_SERVER_ERROR("GE00001", "오류가 발생하였습니다."),
    INTER_SERVER_ERROR,
    NOT_FOUND,
    FORBIDDEN,
    ACCESS_DENIED,
    UNAUTHORIZED,
    // USER, LOGIN
    USER_NOT_FOUND("IE00001", "아이디가 틀렸거나 등록 되어있지 않습니다."),
    ACCESS_TOKEN_EXPIRED("IE00002", "토큰이 만료되었습니다.");

    private String code;
    private String message;

    ErrorCode(String code, String message) {
        this.message = message;
        this.code = code;
    }

    // 주어진 msg와 동일한 message를 가진 ErrorCode를 찾는 메서드
    public static ErrorCode findByMessage(String msg) {
        for (ErrorCode errorCode : ErrorCode.values()) {
            if (errorCode.getMessage() != null && errorCode.getMessage().equals(msg)) {
                return errorCode;  // 일치하는 항목을 찾으면 반환
            }
        }
        return INTER_SERVER_ERROR;  // 일치하는 항목이 없으면 null 반환
    }
}