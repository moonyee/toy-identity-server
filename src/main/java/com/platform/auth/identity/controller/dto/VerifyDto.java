package com.platform.auth.identity.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class VerifyDto {

    @Schema(description = "이메일 인증 코드 검증 요청")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerifyRequest {
        @Schema(description = "인증 대상 이메일", example = "alice@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED)
        private String email;

        @Schema(description = "이메일로 발송된 6자리 인증 코드", example = "123456",
            requiredMode = Schema.RequiredMode.REQUIRED, minLength = 6, maxLength = 6)
        private String code;
    }

    @Schema(description = "인증 코드 재발송 요청")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResendRequest {
        @Schema(description = "재발송 대상 이메일", example = "alice@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED)
        private String email;
    }
}
