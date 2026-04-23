package com.platform.auth.identity.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class WithdrawDto {

    /**
     * {@code DELETE /api/v1/auth/me} 요청 바디.
     * 현재 로그인한 사용자의 비밀번호를 다시 받아 세션 하이재킹 악용을 막는다.
     */
    @Schema(description = "회원 탈퇴 요청 — 비밀번호 재확인")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @Schema(description = "현재 비밀번호(평문). 불일치 시 탈퇴 거부.", example = "P@ssw0rd!23",
            requiredMode = Schema.RequiredMode.REQUIRED)
        private String password;
    }
}
