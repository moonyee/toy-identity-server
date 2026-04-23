package com.platform.auth.identity.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class JoinDto {

	@Schema(description = "회원가입 요청")
	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Request {
		@Schema(description = "사용자 아이디(중복 불가)", example = "alice",
			requiredMode = Schema.RequiredMode.REQUIRED)
		private String userId;

		@Schema(description = "비밀번호(평문, 서버에서 BCrypt 해싱)", example = "P@ssw0rd!23",
			requiredMode = Schema.RequiredMode.REQUIRED)
		private String password;

		@Schema(description = "사용자 이름", example = "앨리스",
			requiredMode = Schema.RequiredMode.REQUIRED)
		private String userName;

		@Schema(description = "이메일 주소(인증 코드 수신처, 중복 불가)", example = "alice@example.com",
			requiredMode = Schema.RequiredMode.REQUIRED)
		private String email;
	}

	@Schema(description = "회원가입 성공 응답")
	@Getter
	public static class Response {
		@Schema(description = "생성된 사용자 아이디", example = "alice")
		private String userId;

		@Schema(description = "안내 메시지", example = "인증 메일이 발송되었습니다. 3분 내에 인증 코드를 입력해 주세요.")
		private String message;

		public Response(String userId, String message) {
			this.userId = userId;
			this.message = message;
		}
	}
}
