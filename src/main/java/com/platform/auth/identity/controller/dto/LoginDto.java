package com.platform.auth.identity.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class LoginDto {

	@Schema(description = "로그인 요청")
	@Getter
	@NoArgsConstructor
	public static class Request {
		@Schema(description = "사용자 아이디", example = "alice", requiredMode = Schema.RequiredMode.REQUIRED)
		private String userId;

		@Schema(description = "비밀번호(평문)", example = "P@ssw0rd!23", requiredMode = Schema.RequiredMode.REQUIRED)
		private String password;
	}

	@Schema(description = "로그인 성공 응답")
	@Getter
	public static class Response {
		@Schema(description = "발급된 JWT access token (HS256, 1시간 유효)",
			example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhbGljZSJ9.xxx")
		private String token;

		public Response(String token) { this.token = token; }
	}
}
