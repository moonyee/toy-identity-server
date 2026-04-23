package com.platform.auth.identity.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class JoinDto {

	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Request {
		private String userId;
		private String password;
		private String userName;
		private String email; // 통으로 받기
	}

	@Getter
	public static class Response {
		private String userId;
		private String message;
		public Response(String userId, String message) {
			this.userId = userId;
			this.message = message;
		}
	}
}