package com.platform.auth.identity.exception;

import lombok.Getter;

@Getter
public class ErrorException extends RuntimeException {
    private ErrorCode errorCode;
    private String code;
    private String message;

    public ErrorException(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    public ErrorException(ErrorCode errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
    }

    public ErrorException(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
