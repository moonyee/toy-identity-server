package com.platform.auth.identity.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.platform.auth.identity.common.response.ApiResponseEntity;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ErrorException.class)
    protected ResponseEntity<ApiResponseEntity<ErrorResponseData>> handleErrorException(ErrorException e) {
        log.error("### Business Exception: {}", e.getMessage());

        ErrorCode errorCode = e.getErrorCode();
        ErrorResponseData errorData = ErrorResponseData.of(errorCode);

        // 상세 메시지가 있으면 추가
        if (e.getMessage() != null && !e.getMessage().isEmpty()) {
            errorData.setMessage(e.getMessage());
        }

        // ErrorCode에 정의된 HttpStatus가 있다면 그것을 사용하고, 없으면 500 사용
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponseEntity.error(errorData, e.getMessage()));
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

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResponseEntity<ErrorResponseData>> handleGeneralException(Exception e) {
        log.error("### Unexpected Exception: ", e); // StackTrace를 위해 e 전체를 로그에 남김

        ErrorResponseData errorData = ErrorResponseData.of(ErrorCode.TEMPORARY_SERVER_ERROR);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponseEntity.error(errorData, "서버 내부 오류가 발생했습니다."));
    }

    // @ExceptionHandler(JwtException.class)
    // protected ResponseEntity<ApiResponseEntity<ErrorResponseData>> handleJwtException(JwtException e) {
    //     log.error("### JWT Exception in Controller Layer: {}", e.getMessage());
    //
    //     // ErrorCode에 UNAUTHORIZED(401) 등이 정의되어 있다고 가정
    //     ErrorResponseData errorData = ErrorResponseData.of(ErrorCode.UNAUTHORIZED);
    //     errorData.setMessage(e.getMessage());
    //
    //     return ResponseEntity
    //         .status(HttpStatus.UNAUTHORIZED)
    //         .body(ApiResponseEntity.error(errorData, e.getMessage()));
    // }
}
