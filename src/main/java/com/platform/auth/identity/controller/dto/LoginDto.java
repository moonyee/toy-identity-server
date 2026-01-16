package com.platform.auth.identity.controller.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

public class LoginDto {

	@Getter
	@NoArgsConstructor
	public static class Request {
		private String userId;
		private String password;
	}

	@Getter
	public static class Response {
		private String token;
		public Response(String token) { this.token = token; }
	}
}