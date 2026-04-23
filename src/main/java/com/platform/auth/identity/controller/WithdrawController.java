package com.platform.auth.identity.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.platform.auth.identity.common.response.ApiResponseEntity;
import com.platform.auth.identity.controller.dto.WithdrawDto;
import com.platform.auth.identity.exception.ErrorCode;
import com.platform.auth.identity.exception.ErrorException;
import com.platform.auth.identity.service.WithdrawService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 회원 탈퇴 엔드포인트.
 * <p>
 * 경로: {@code DELETE /api/v1/auth/me} (Gateway 기준) → context-path {@code /api} 제거 후 {@code /v1/auth/me}.
 * </p>
 * <p>
 * Bearer 인증이 필수이며 {@code JwtAuthenticationFilter}가 {@link SecurityContextHolder}에
 * userId를 주입한 상태를 전제로 한다. body에는 {@link WithdrawDto.Request#getPassword()} 로 비밀번호 재확인을 받는다.
 * </p>
 */
@Slf4j
@Tag(name = "Auth - Withdraw", description = "회원 탈퇴 (Soft delete)")
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class WithdrawController {

    private final WithdrawService withdrawService;

    @Operation(
        summary = "회원 탈퇴",
        description = "현재 인증된 사용자를 Soft delete 처리한다. 상태가 DELETED로 전이되고 withdrawnAt이 기록되며, 해당 사용자의 모든 활성 세션(AUTH:*)과 진행 중 이메일 인증 코드(auth:code:*)가 즉시 삭제된다. 비밀번호 재확인이 필수."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "탈퇴 완료"),
        @ApiResponse(responseCode = "401", description = "인증 정보 없음 / 비밀번호 불일치 / 이미 탈퇴한 계정(은폐)")
    })
    @DeleteMapping("/me")
    public ApiResponseEntity<String> withdraw(@RequestBody WithdrawDto.Request request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ErrorException(ErrorCode.UNAUTHORIZED, "인증 정보를 찾을 수 없습니다.");
        }
        String userId = auth.getName();

        log.info("### Withdraw attempt userId={}", userId);
        withdrawService.withdraw(userId, request.getPassword());

        return ApiResponseEntity.success("회원 탈퇴가 완료되었습니다.");
    }
}
