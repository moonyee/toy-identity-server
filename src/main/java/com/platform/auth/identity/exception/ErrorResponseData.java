package com.platform.auth.identity.exception;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ErrorResponseData {
    private String code;
    private String message;

    public ErrorResponseData(ErrorCode code) {
        this.code = code.getCode();
        this.message = code.getMessage();
    }

    public void setErrorCode(ErrorCode code) {
        this.code = code.getCode();
        this.message = code.getMessage();
    }

    public static ErrorResponseData of(ErrorCode code) {
        return new ErrorResponseData(code);
    }
}
