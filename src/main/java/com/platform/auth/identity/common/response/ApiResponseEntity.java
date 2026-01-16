package com.platform.auth.identity.common.response;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.Builder;

@Builder // 빌더 패턴을 섞으면 가독성이 더 좋아집니다.
public record ApiResponseEntity<T>(
	Status status,
	T data,
	String message
) {
	public enum Status {
		SUCCESS("success"),
		FAIL("fail"),
		ERROR("error");

		@JsonValue
		private final String status;

		Status(String status) { this.status = status; }
	}

	// 성공 응답
	public static <T> ApiResponseEntity<T> success(T data) {
		return new ApiResponseEntity<>(Status.SUCCESS, data, null);
	}

	// 성공시 리턴 값 (data 무)
	public static ApiResponseEntity<?> createSuccessWithoutContent() {
		return new ApiResponseEntity<>(Status.SUCCESS, null, null);
	}

	// 성공시 리턴 값 (data 무)
	public static ApiResponseEntity<?> createSuccessWithoutContent(String message) {
		return new ApiResponseEntity<>(Status.SUCCESS, null, message);
	}

	// 비즈니스 로직 실패 (예: 로그인 실패)
	public static <T> ApiResponseEntity<T> fail(String message) {
		return new ApiResponseEntity<>(Status.FAIL, null, message);
	}

	public static <T> ApiResponseEntity<T> error(T data, String message) {
		return new ApiResponseEntity<>(Status.ERROR, data, message);
	}

	public static <T> ApiResponseEntity<T> error(T data) {
		return new ApiResponseEntity<>(Status.ERROR, data, null);
	}
}